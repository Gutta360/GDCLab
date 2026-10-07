package com.gdc.backend.registration.service;

import com.gdc.backend.branch.entity.Branch;
import com.gdc.backend.branch.service.BranchService;
import com.gdc.backend.common.dto.PageResponse;
import com.gdc.backend.common.exception.*;
import com.gdc.backend.common.money.Money;
import com.gdc.backend.patient.entity.*;
import com.gdc.backend.patient.repository.PatientRepository;
import com.gdc.backend.payment.entity.Payment;
import com.gdc.backend.payment.repository.PaymentRepository;
import com.gdc.backend.referral.entity.*;
import com.gdc.backend.referral.service.ReferralService;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.*;
import com.gdc.backend.registration.mapper.RegistrationMapper;
import com.gdc.backend.registration.repository.*;
import com.gdc.backend.testmaster.entity.DiagnosticTest;
import com.gdc.backend.testmaster.service.DiagnosticTestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final RegistrationTestRepository registrationTests;
    private final RegistrationSearchRepository searchRepository;
    private final PatientRepository patients;
    private final BranchService branches;
    private final ReferralService referrals;
    private final DiagnosticTestService testMaster;
    private final PaymentRepository payments;
    private final RegistrationMapper mapper;
    private final Clock clock;

    public RegistrationService(RegistrationRepository registrations, RegistrationTestRepository registrationTests,
            RegistrationSearchRepository searchRepository, PatientRepository patients, BranchService branches,
            ReferralService referrals, DiagnosticTestService testMaster, PaymentRepository payments,
            RegistrationMapper mapper, Clock clock) {
        this.registrations=registrations; this.registrationTests=registrationTests; this.searchRepository=searchRepository;
        this.patients=patients; this.branches=branches; this.referrals=referrals; this.testMaster=testMaster;
        this.payments=payments; this.mapper=mapper; this.clock=clock;
    }

    public RegistrationResponse create(RegistrationCreateRequest request) {
        Instant registeredAt = request.registrationDatetime()==null ? clock.instant() : request.registrationDatetime();
        Patient patient = resolvePatient(request.patient(), registeredAt);
        Branch branch = branches.requireActive(request.branchId());
        ReferralSource doctor = referrals.requireActiveType(request.referringDoctorId(), ReferralType.DOCTOR);
        ReferralSource lab = referrals.requireActiveType(request.referringLabId(), ReferralType.LAB);
        validateDistinctTests(request.tests());

        Registration registration = new Registration();
        registration.setRegistrationNo(format("REG-", registrations.nextRegistrationNumber()));
        registration.setBillNo(format("BILL-", registrations.nextBillNumber()));
        registration.setPatient(patient); registration.setBranch(branch); registration.setRegistrationDatetime(registeredAt);
        registration.setReferringDoctor(doctor); registration.setReferringLab(lab);
        snapshots(registration, patient, branch, doctor, lab);

        BigDecimal billAmount=Money.ZERO;
        int displayOrder=1;
        for (RegistrationTestRequest selected:request.tests()) {
            DiagnosticTest master=testMaster.requireActive(selected.testCode());
            BigDecimal price=master.getCurrentPrice().setScale(2);
            BigDecimal percentage=Money.percentage(selected.discountPercentage(),"Test discount percentage");
            BigDecimal discount=Money.discount(price,percentage);
            BigDecimal total=price.subtract(discount);
            RegistrationTest line=new RegistrationTest();
            line.setTest(master); line.setTestNameSnapshot(master.getTestName());
            line.setCategoryCodeSnapshot(master.getCategory().getCategoryCode());
            line.setCategoryNameSnapshot(master.getCategory().getCategoryName());
            line.setTestPrice(price); line.setDiscountPercentage(percentage); line.setDiscountAmount(discount);
            line.setTotalAmount(total); line.setDisplayOrder(displayOrder++); line.setStatus(TestStatus.REGISTERED);
            line.setRegisteredAt(registeredAt); registration.addTest(line); billAmount=billAmount.add(total);
        }
        BigDecimal billDiscountPercentage=Money.percentage(request.discountPercentage(),"Bill discount percentage");
        BigDecimal billDiscount=Money.discount(billAmount,billDiscountPercentage);
        BigDecimal finalAmount=billAmount.subtract(billDiscount);
        BigDecimal initialAmount=request.initialPayment()==null?Money.ZERO:Money.amount(request.initialPayment().amount(),"Payment amount");
        if (initialAmount.compareTo(finalAmount)>0) throw new BusinessValidationException("Payment cannot exceed the amount due");
        registration.setBillAmount(billAmount); registration.setDiscountPercentage(billDiscountPercentage);
        registration.setDiscountAmount(billDiscount); registration.setFinalBillAmount(finalAmount);
        registration.setPaidAmount(initialAmount); registration.setDueAmount(finalAmount.subtract(initialAmount));
        registration.setStatus(status(initialAmount,finalAmount.subtract(initialAmount)));
        registration.setRemarks(optional(request.remarks()));
        Registration saved=registrations.saveAndFlush(registration);

        if (initialAmount.signum()>0) {
            Payment payment=new Payment(); payment.setPaymentNo(format("PAY-",payments.nextPaymentNumber()));
            payment.setRegistration(saved); payment.setAmount(initialAmount); payment.setPaymentMode(request.initialPayment().paymentMode());
            payment.setPaymentDatetime(registeredAt); payment.setReferenceNo(optional(request.initialPayment().referenceNo()));
            payment.setRemarks(optional(request.initialPayment().remarks())); payments.saveAndFlush(payment);
            reconcile(saved);
        }
        return mapper.response(saved,payments.findAllByRegistrationIdOrderByPaymentDatetimeAscIdAsc(saved.getId()));
    }

    @Transactional(readOnly=true)
    public RegistrationResponse get(String registrationNo) {
        Registration r=require(registrationNo);
        return mapper.response(r,payments.findAllByRegistrationIdOrderByPaymentDatetimeAscIdAsc(r.getId()));
    }

    @Transactional(readOnly=true)
    public BillResponse bill(String registrationNo) { return mapper.bill(require(registrationNo)); }

    @Transactional(readOnly=true)
    public PageResponse<RegistrationSearchRow> search(String registrationNo,String billNo,String patientId,String patientName,
            String mobile,Long branchId,LocalDate fromDate,LocalDate toDate,int page,int size) {
        return searchRepository.search(registrationNo,billNo,patientId,patientName,mobile,branchId,fromDate,toDate,page,size);
    }

    public RegistrationTestResponse updateTestStatus(String registrationNo,String testCode,TestStatusUpdateRequest request) {
        RegistrationTest test=registrationTests.findForStatusUpdate(normalizeId(registrationNo),testCode)
                .orElseThrow(()->new ResourceNotFoundException("Registration test not found"));
        if (!Objects.equals(test.getVersion(),request.expectedVersion())) throw new BusinessConflictException("Test status changed; refresh and try again");
        if (!TestStatusTransitions.allows(test.getStatus(),request.status())) throw new BusinessValidationException("Invalid test status transition from "+test.getStatus()+" to "+request.status());
        test.setStatus(request.status());
        if(request.status()==TestStatus.COMPLETED) test.setCompletedAt(clock.instant());
        else { test.setCompletedAt(null); test.setCompletedBy(null); }
        return mapper.test(registrationTests.saveAndFlush(test));
    }

    Registration require(String number) {
        return registrations.findByRegistrationNo(normalizeId(number))
                .orElseThrow(()->new ResourceNotFoundException("Registration not found: "+number));
    }

    static RegistrationStatus status(BigDecimal paid,BigDecimal due) {
        if(due.signum()==0)return RegistrationStatus.PAID;
        if(paid.signum()==0)return RegistrationStatus.OPEN;
        return RegistrationStatus.PARTIALLY_PAID;
    }

    void reconcile(Registration registration) {
        BigDecimal paid=payments.sumByRegistrationId(registration.getId()).setScale(2);
        BigDecimal due=registration.getFinalBillAmount().subtract(paid);
        if(due.signum()<0)throw new BusinessValidationException("Persisted payments exceed the final bill amount");
        registration.setPaidAmount(paid);registration.setDueAmount(due);registration.setStatus(status(paid,due));
        registrations.saveAndFlush(registration);
    }

    private Patient resolvePatient(RegistrationPatientRequest request,Instant visitTime) {
        if(request.patientId()!=null&&!request.patientId().isBlank()) {
            Patient patient=patients.findByPatientIdAndActiveTrue(normalizeId(request.patientId()))
                    .orElseThrow(()->new ResourceNotFoundException("Patient not found: "+request.patientId()));
            if(request.updateExisting()) applyPatient(patient,request,false);
            return patient;
        }
        String first=request.firstName().trim(); String mobile=request.mobile().trim();
        if(patients.existsByFirstNameAndMobile(first,mobile)) throw new BusinessConflictException("A patient with the same first name and mobile number already exists");
        Patient patient=new Patient(); patient.setPatientId(format("P-",patients.getNextPatientNumber()));
        patient.setReferredBy(ReferredBy.SELF);patient.setDoctorName(null);patient.setConsultationFee(Money.ZERO);
        patient.setRegistrationDate(visitTime);patient.setActive(true);applyPatient(patient,request,true);
        return patients.saveAndFlush(patient);
    }

    private void applyPatient(Patient patient,RegistrationPatientRequest r,boolean creating) {
        String first=r.firstName().trim();String mobile=r.mobile().trim();
        if(!creating&&patients.existsByFirstNameAndMobileAndPatientIdNot(first,mobile,patient.getPatientId()))
            throw new BusinessConflictException("Another patient with the same first name and mobile number already exists");
        patient.setTitle(optional(r.title()));patient.setFirstName(first);patient.setLastName(optional(r.lastName()));
        patient.setGender(r.gender());patient.setMobile(mobile);patient.setAddress(optional(r.address()));patient.setEmail(optional(r.email()));
        patient.setDateOfBirth(r.dateOfBirth());patient.setAge(deriveAge(r.dateOfBirth(),r.age()));
        if(creating||r.aadhaarNo()!=null)patient.setAadhaarNo(optional(r.aadhaarNo()));
    }

    private short deriveAge(LocalDate dob,Short supplied) {
        if(dob==null)return supplied;
        LocalDate today=LocalDate.now(clock);if(dob.isAfter(today))throw new BusinessValidationException("Date of birth cannot be in the future");
        int age=Period.between(dob,today).getYears();if(age>120)throw new BusinessValidationException("Derived age must not exceed 120");
        return (short)age;
    }

    private void snapshots(Registration r,Patient p,Branch b,ReferralSource doctor,ReferralSource lab) {
        r.setPatientTitleSnapshot(p.getTitle());r.setPatientNameSnapshot((p.getFirstName()+" "+Optional.ofNullable(p.getLastName()).orElse("")).trim());
        r.setPatientAgeSnapshot(p.getAge());r.setPatientGenderSnapshot(p.getGender());r.setPatientMobileSnapshot(p.getMobile());
        r.setPatientAddressSnapshot(p.getAddress());r.setBranchCodeSnapshot(b.getBranchCode());r.setBranchNameSnapshot(b.getBranchName());
        r.setBranchAddressSnapshot(b.getAddress());r.setBranchPhoneSnapshot(b.getPhone());r.setBranchMobileSnapshot(b.getMobile());
        if(doctor!=null){r.setReferringDoctorNameSnapshot(doctor.getName());r.setReferringDoctorQualificationSnapshot(doctor.getQualification());}
        if(lab!=null)r.setReferringLabNameSnapshot(lab.getName());
    }

    private void validateDistinctTests(List<RegistrationTestRequest> selected) {
        if (selected == null || selected.isEmpty()) {
            throw new BusinessValidationException("At least one diagnostic test is required");
        }
        Set<String> codes=new HashSet<>();for(var t:selected){String code=normalizeId(t.testCode());if(!codes.add(code))throw new BusinessValidationException("Duplicate diagnostic test: "+code);}
    }
    private String format(String prefix,Long number){return prefix+"%05d".formatted(number);}
    private String normalizeId(String value){if(value==null||value.isBlank())throw new BusinessValidationException("Business identifier is required");return value.trim().toUpperCase();}
    private String optional(String value){if(value==null)return null;String v=value.trim();return v.isEmpty()?null:v;}
}

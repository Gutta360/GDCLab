package com.gdc.backend.payment.service;

import com.gdc.backend.common.dto.PagedReportResponse;
import com.gdc.backend.common.exception.*;
import com.gdc.backend.common.money.Money;
import com.gdc.backend.payment.dto.*;
import com.gdc.backend.payment.entity.Payment;
import com.gdc.backend.payment.repository.*;
import com.gdc.backend.registration.entity.*;
import com.gdc.backend.registration.mapper.RegistrationMapper;
import com.gdc.backend.registration.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;

@Service
@Transactional
public class PaymentService {
    private final RegistrationRepository registrations;
    private final PaymentRepository payments;
    private final PaymentSearchRepository searchRepository;
    private final RegistrationMapper mapper;
    private final Clock clock;
    public PaymentService(RegistrationRepository registrations,PaymentRepository payments,
            PaymentSearchRepository searchRepository,RegistrationMapper mapper,Clock clock){
        this.registrations=registrations;this.payments=payments;this.searchRepository=searchRepository;this.mapper=mapper;this.clock=clock;
    }
    public PaymentResponse create(String registrationNo,PaymentCreateRequest request){
        String normalized=registrationNo==null?"":registrationNo.trim().toUpperCase();
        Registration registration=registrations.findByRegistrationNoForUpdate(normalized)
                .orElseThrow(()->new ResourceNotFoundException("Registration not found: "+registrationNo));
        if(registration.getStatus()==RegistrationStatus.CANCELLED)throw new BusinessValidationException("Payments cannot be added to a cancelled Registration");
        BigDecimal currentPaid=payments.sumByRegistrationId(registration.getId()).setScale(2);
        BigDecimal due=registration.getFinalBillAmount().subtract(currentPaid);
        BigDecimal amount=Money.amount(request.amount(),"Payment amount");
        if(amount.signum()<=0)throw new BusinessValidationException("Payment amount must be greater than zero");
        if(amount.compareTo(due)>0)throw new BusinessValidationException("Payment cannot exceed the current amount due");
        Payment payment=new Payment();payment.setPaymentNo("PAY-"+"%05d".formatted(payments.nextPaymentNumber()));
        payment.setRegistration(registration);payment.setAmount(amount);payment.setPaymentMode(request.paymentMode());
        payment.setPaymentDatetime(clock.instant());payment.setReferenceNo(optional(request.referenceNo()));payment.setRemarks(optional(request.remarks()));
        Payment saved=payments.saveAndFlush(payment);
        BigDecimal persisted=payments.sumByRegistrationId(registration.getId()).setScale(2);
        BigDecimal remaining=registration.getFinalBillAmount().subtract(persisted);
        registration.setPaidAmount(persisted);registration.setDueAmount(remaining);
        registration.setStatus(remaining.signum()==0?RegistrationStatus.PAID:RegistrationStatus.PARTIALLY_PAID);
        registrations.saveAndFlush(registration);
        return mapper.payment(saved);
    }
    @Transactional(readOnly=true)
    public PagedReportResponse<PaymentSearchRow,PaymentSearchSummary> search(String paymentNo,String billNo,
            String registrationNo,String patientName,String mobile,com.gdc.backend.payment.entity.PaymentMode mode,
            Long branchId,LocalDate fromDate,LocalDate toDate,int page,int size){
        return searchRepository.search(paymentNo,billNo,registrationNo,patientName,mobile,mode,branchId,fromDate,toDate,page,size);
    }
    private String optional(String value){if(value==null)return null;String v=value.trim();return v.isEmpty()?null:v;}
}

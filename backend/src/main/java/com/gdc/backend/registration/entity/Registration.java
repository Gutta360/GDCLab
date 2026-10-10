package com.gdc.backend.registration.entity;

import com.gdc.backend.branch.entity.Branch;
import com.gdc.backend.common.entity.AuditedEntity;
import com.gdc.backend.referral.entity.ReferralSource;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations")
public class Registration extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "registration_no", nullable = false, unique = true, length = 20) private String registrationNo;
    @Column(name = "bill_no", nullable = false, unique = true, length = 20) private String billNo;
    @Column(name = "patient_title", length = 20) private String patientTitle;
    @Column(name = "patient_name", nullable = false, length = 101) private String patientName;
    @Column(name = "patient_mobile", nullable = false, length = 10) private String patientMobile;
    @Column(name = "patient_aadhaar", length = 12) private String patientAadhaar;
    @Enumerated(EnumType.STRING) @Column(name = "patient_gender", nullable = false, length = 10) private Gender patientGender;
    @Column(name = "patient_email", length = 254) private String patientEmail;
    @Column(name = "patient_date_of_birth") private LocalDate patientDateOfBirth;
    @Column(name = "patient_age", nullable = false) private Short patientAge;
    @Column(name = "patient_address", length = 255) private String patientAddress;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "registration_datetime", nullable = false) private Instant registrationDatetime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "referring_doctor_id") private ReferralSource referringDoctor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "referring_lab_id") private ReferralSource referringLab;

    @Column(name = "branch_code_snapshot", nullable = false, length = 20) private String branchCodeSnapshot;
    @Column(name = "branch_name_snapshot", nullable = false, length = 150) private String branchNameSnapshot;
    @Column(name = "branch_address_snapshot", nullable = false, length = 500) private String branchAddressSnapshot;
    @Column(name = "branch_phone_snapshot", length = 20) private String branchPhoneSnapshot;
    @Column(name = "branch_mobile_snapshot", length = 20) private String branchMobileSnapshot;
    @Column(name = "referring_doctor_name_snapshot", length = 150) private String referringDoctorNameSnapshot;
    @Column(name = "referring_doctor_qualification_snapshot", length = 100) private String referringDoctorQualificationSnapshot;
    @Column(name = "referring_lab_name_snapshot", length = 150) private String referringLabNameSnapshot;

    @Column(name = "bill_amount", nullable = false, precision = 10, scale = 2) private BigDecimal billAmount;
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2) private BigDecimal discountPercentage;
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2) private BigDecimal discountAmount;
    @Column(name = "final_bill_amount", nullable = false, precision = 10, scale = 2) private BigDecimal finalBillAmount;
    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2) private BigDecimal paidAmount;
    @Column(name = "due_amount", nullable = false, precision = 10, scale = 2) private BigDecimal dueAmount;
    @Column(length = 1000) private String remarks;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RegistrationStatus status;
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC") private List<RegistrationTest> tests = new ArrayList<>();

    public Long getId() { return id; }
    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String value) { registrationNo = value; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String value) { billNo = value; }
    public String getPatientTitle() { return patientTitle; }
    public void setPatientTitle(String value) { patientTitle = value; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String value) { patientName = value; }
    public String getPatientMobile() { return patientMobile; }
    public void setPatientMobile(String value) { patientMobile = value; }
    public String getPatientAadhaar() { return patientAadhaar; }
    public void setPatientAadhaar(String value) { patientAadhaar = value; }
    public Gender getPatientGender() { return patientGender; }
    public void setPatientGender(Gender value) { patientGender = value; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String value) { patientEmail = value; }
    public LocalDate getPatientDateOfBirth() { return patientDateOfBirth; }
    public void setPatientDateOfBirth(LocalDate value) { patientDateOfBirth = value; }
    public Short getPatientAge() { return patientAge; }
    public void setPatientAge(Short value) { patientAge = value; }
    public String getPatientAddress() { return patientAddress; }
    public void setPatientAddress(String value) { patientAddress = value; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch value) { branch = value; }
    public Instant getRegistrationDatetime() { return registrationDatetime; }
    public void setRegistrationDatetime(Instant value) { registrationDatetime = value; }
    public ReferralSource getReferringDoctor() { return referringDoctor; }
    public void setReferringDoctor(ReferralSource value) { referringDoctor = value; }
    public ReferralSource getReferringLab() { return referringLab; }
    public void setReferringLab(ReferralSource value) { referringLab = value; }
    public String getBranchCodeSnapshot() { return branchCodeSnapshot; }
    public void setBranchCodeSnapshot(String value) { branchCodeSnapshot = value; }
    public String getBranchNameSnapshot() { return branchNameSnapshot; }
    public void setBranchNameSnapshot(String value) { branchNameSnapshot = value; }
    public String getBranchAddressSnapshot() { return branchAddressSnapshot; }
    public void setBranchAddressSnapshot(String value) { branchAddressSnapshot = value; }
    public String getBranchPhoneSnapshot() { return branchPhoneSnapshot; }
    public void setBranchPhoneSnapshot(String value) { branchPhoneSnapshot = value; }
    public String getBranchMobileSnapshot() { return branchMobileSnapshot; }
    public void setBranchMobileSnapshot(String value) { branchMobileSnapshot = value; }
    public String getReferringDoctorNameSnapshot() { return referringDoctorNameSnapshot; }
    public void setReferringDoctorNameSnapshot(String value) { referringDoctorNameSnapshot = value; }
    public String getReferringDoctorQualificationSnapshot() { return referringDoctorQualificationSnapshot; }
    public void setReferringDoctorQualificationSnapshot(String value) { referringDoctorQualificationSnapshot = value; }
    public String getReferringLabNameSnapshot() { return referringLabNameSnapshot; }
    public void setReferringLabNameSnapshot(String value) { referringLabNameSnapshot = value; }
    public BigDecimal getBillAmount() { return billAmount; }
    public void setBillAmount(BigDecimal value) { billAmount = value; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal value) { discountPercentage = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { discountAmount = value; }
    public BigDecimal getFinalBillAmount() { return finalBillAmount; }
    public void setFinalBillAmount(BigDecimal value) { finalBillAmount = value; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal value) { paidAmount = value; }
    public BigDecimal getDueAmount() { return dueAmount; }
    public void setDueAmount(BigDecimal value) { dueAmount = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus value) { status = value; }
    public List<RegistrationTest> getTests() { return tests; }
    public void addTest(RegistrationTest test) { tests.add(test); test.setRegistration(this); }
}

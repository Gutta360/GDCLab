package com.gdc.backend.registration.dto;

import com.gdc.backend.patient.entity.Gender;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BillResponse(
        String branchName, String branchAddress, String branchPhone, String branchMobile,
        String registrationNo, String billNo, Instant registrationDatetime,
        String patientTitle, String patientName, Short patientAge, Gender patientGender,
        String patientMobile, String patientAddress,
        String referringDoctorName, String referringDoctorQualification, String referringLabName,
        List<BillTestLine> tests, BigDecimal billAmount, BigDecimal discountPercentage,
        BigDecimal discountAmount, BigDecimal finalBillAmount, BigDecimal paidAmount,
        BigDecimal dueAmount, String preparedBy
) {
    public record BillTestLine(Integer sequence, String testName, BigDecimal testAmount,
                               BigDecimal discountPercentage, BigDecimal discountAmount,
                               BigDecimal totalAmount) {}
}

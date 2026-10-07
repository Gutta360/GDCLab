package com.gdc.backend.registration.dto;

import com.gdc.backend.patient.entity.Gender;
import com.gdc.backend.payment.dto.PaymentResponse;
import com.gdc.backend.registration.entity.RegistrationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RegistrationResponse(
        String registrationNo, String billNo, Instant registrationDatetime,
        String patientId, String patientTitle, String patientName, Short patientAge,
        Gender patientGender, String patientMobile, String patientAddress,
        Long branchId, String branchCode, String branchName,
        Long referringDoctorId, String referringDoctorName, String referringDoctorQualification,
        Long referringLabId, String referringLabName,
        List<RegistrationTestResponse> tests, BigDecimal billAmount,
        BigDecimal discountPercentage, BigDecimal discountAmount, BigDecimal finalBillAmount,
        BigDecimal paidAmount, BigDecimal dueAmount, RegistrationStatus status,
        String remarks, List<PaymentResponse> payments, Long version
) {}

package com.gdc.backend.registration.dto;

import com.gdc.backend.registration.entity.RegistrationStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RegistrationSearchRow(String registrationNo, String billNo, Instant registrationDatetime,
        String patientId, String patientName, String mobile, long testCount,
        BigDecimal billAmount, BigDecimal paidAmount, BigDecimal dueAmount,
        RegistrationStatus status, String branchCode, String branchName) {}

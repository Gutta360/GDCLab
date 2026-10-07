package com.gdc.backend.registration.dto;

import com.gdc.backend.registration.entity.TestStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RegistrationTestSearchRow(Instant businessDatetime, String testCode, String testName,
        String categoryCode, String categoryName, TestStatus status, String registrationNo,
        String billNo, String patientName, BigDecimal testAmount, BigDecimal discountAmount,
        BigDecimal finalTestAmount, String branchCode, String branchName) {}

package com.gdc.backend.registration.dto;

import com.gdc.backend.registration.entity.TestStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RegistrationTestResponse(
        String testCode, String testName, String categoryCode, String categoryName,
        BigDecimal testPrice, BigDecimal discountPercentage, BigDecimal discountAmount,
        BigDecimal totalAmount, Integer displayOrder, TestStatus status,
        Instant registeredAt, Instant completedAt, Long version
) {}

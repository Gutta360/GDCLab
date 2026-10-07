package com.gdc.backend.registration.dto;

import java.math.BigDecimal;

public record TestSearchSummary(long totalTests, BigDecimal grossAmount,
                                BigDecimal testDiscountAmount, BigDecimal netTestAmount) {}

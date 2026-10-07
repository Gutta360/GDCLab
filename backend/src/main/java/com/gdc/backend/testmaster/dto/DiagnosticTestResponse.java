package com.gdc.backend.testmaster.dto;

import java.math.BigDecimal;

public record DiagnosticTestResponse(String testCode, String testName, String categoryCode,
                                     String categoryName, BigDecimal currentPrice,
                                     Integer displayOrder, boolean active) {}

package com.gdc.backend.registration.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RegistrationTestRequest(
        @NotBlank String testCode,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal discountPercentage
) {}

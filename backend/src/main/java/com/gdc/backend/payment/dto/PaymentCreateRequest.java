package com.gdc.backend.payment.dto;

import com.gdc.backend.payment.entity.PaymentMode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentCreateRequest(
        @NotNull @DecimalMin(value="0.00", inclusive=false) @Digits(integer=8, fraction=2) BigDecimal amount,
        @NotNull PaymentMode paymentMode,
        @Size(max=100) String referenceNo,
        @Size(max=1000) String remarks
) {}

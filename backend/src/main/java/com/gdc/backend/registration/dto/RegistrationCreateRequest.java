package com.gdc.backend.registration.dto;

import com.gdc.backend.payment.dto.PaymentCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RegistrationCreateRequest(
        @NotNull @Valid RegistrationPatientRequest patient,
        @NotNull Long branchId,
        Long referringDoctorId,
        Long referringLabId,
        Instant registrationDatetime,
        @NotEmpty List<@Valid RegistrationTestRequest> tests,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal discountPercentage,
        @Valid PaymentCreateRequest initialPayment,
        @Size(max=1000) String remarks
) {}

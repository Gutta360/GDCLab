package com.gdc.backend.registration.dto;

import com.gdc.backend.payment.dto.PaymentCreateRequest;
import com.gdc.backend.registration.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RegistrationCreateRequest(
        @Size(max=20) String patientTitle,
        @NotBlank @Size(min=2,max=101) @Pattern(regexp="^[A-Za-z ]+$") String patientName,
        @NotBlank @Pattern(regexp="^[0-9]{10}$") String patientMobile,
        @Pattern(regexp="^$|^[0-9]{12}$") String patientAadhaar,
        @NotNull Gender patientGender,
        @Email @Size(max=254) String patientEmail,
        @PastOrPresent LocalDate patientDateOfBirth,
        @NotNull @Min(0) @Max(120) Short patientAge,
        @Size(max=255) String patientAddress,
        @NotNull Long branchId,
        Long referringDoctorId,
        Long referringLabId,
        Instant registrationDatetime,
        @NotEmpty List<@Valid RegistrationTestRequest> tests,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal discountPercentage,
        @Valid PaymentCreateRequest initialPayment,
        @Size(max=1000) String remarks
) {}

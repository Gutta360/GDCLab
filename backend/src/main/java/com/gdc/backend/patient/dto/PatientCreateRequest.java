package com.gdc.backend.patient.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

import com.gdc.backend.patient.entity.Gender;
import com.gdc.backend.patient.entity.ReferredBy;

public record PatientCreateRequest(

        @NotBlank(message = "First name is required")
        @Size(
                min = 2,
                max = 50,
                message = "First name must be between 2 and 50 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z ]+$",
                message = "First name must contain only letters and spaces"
        )
        String firstName,

        @Size(
                max = 50,
                message = "Last name must not exceed 50 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z ]*$",
                message = "Last name must contain only letters and spaces"
        )
        String lastName,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotNull(message = "Age is required")
        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 120, message = "Age must not exceed 120")
        Short age,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Mobile number must contain exactly 10 digits"
        )
        String mobile,

        @NotBlank(message = "Address is required")
        @Size(
                min = 2,
                max = 255,
                message = "Address must be between 2 and 255 characters"
        )
        String address,

        @NotNull(message = "Referred by is required")
        ReferredBy referredBy,

        @Size(
                max = 100,
                message = "Doctor name must not exceed 100 characters"
        )
        String doctorName,

        @NotNull(message = "Consultation fee is required")
        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Consultation fee cannot be negative"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "Consultation fee must have at most 8 integer digits and 2 decimal places"
        )
        BigDecimal consultationFee,

        @NotNull(message = "Registration date is required")
        Instant registrationDate

) {
}
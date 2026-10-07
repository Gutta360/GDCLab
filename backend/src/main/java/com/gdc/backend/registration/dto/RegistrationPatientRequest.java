package com.gdc.backend.registration.dto;

import com.gdc.backend.patient.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegistrationPatientRequest(
        String patientId,
        boolean updateExisting,
        @Size(max=20) String title,
        @NotBlank @Size(min=2,max=50) @Pattern(regexp="^[A-Za-z ]+$") String firstName,
        @Size(max=50) @Pattern(regexp="^[A-Za-z ]*$") String lastName,
        @NotNull Gender gender,
        @NotNull @Min(0) @Max(120) Short age,
        @NotBlank @Pattern(regexp="^[0-9]{10}$") String mobile,
        @Size(max=255) String address,
        @Email @Size(max=254) String email,
        @PastOrPresent LocalDate dateOfBirth,
        @Pattern(regexp="^$|^[0-9]{12}$") String aadhaarNo
) {}

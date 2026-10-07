package com.gdc.backend.patient.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gdc.backend.patient.entity.Gender;
import com.gdc.backend.patient.entity.ReferredBy;

public record PatientResponse(

        String patientId,

        String title,

        String firstName,

        String lastName,

        String fullName,

        Gender gender,

        Short age,

        String mobile,

        String address,

        String aadhaarMasked,

        String email,

        LocalDate dateOfBirth,

        ReferredBy referredBy,

        String doctorName,

        BigDecimal consultationFee,

        Instant registrationDate,

        boolean active,

        Instant createdAt,

        Instant updatedAt,

        Long version

) {
}

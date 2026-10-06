package com.gdc.backend.patient.dto;

public record PatientSummaryResponse(
        String patientId,
        String firstName,
        String lastName,
        String fullName,
        String mobile
) {
}

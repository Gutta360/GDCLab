package com.gdc.backend.patient.mapper;

import org.springframework.stereotype.Component;

import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.entity.Patient;

@Component
public class PatientMapper {

    /**
     * Converts the persistence entity into the external API response.
     */
    public PatientResponse toResponse(Patient patient) {

        return new PatientResponse(
                patient.getPatientId(),
                patient.getTitle(),
                patient.getFirstName(),
                patient.getLastName(),
                buildFullName(
                        patient.getFirstName(),
                        patient.getLastName()
                ),
                patient.getGender(),
                patient.getAge(),
                patient.getMobile(),
                patient.getAddress(),
                maskAadhaar(patient.getAadhaarNo()),
                patient.getEmail(),
                patient.getDateOfBirth(),
                patient.getReferredBy(),
                patient.getDoctorName(),
                patient.getConsultationFee(),
                patient.getRegistrationDate(),
                patient.isActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getVersion()
        );
    }

    public PatientSummaryResponse toSummaryResponse(Patient patient) {

        return new PatientSummaryResponse(
                patient.getPatientId(),
                patient.getFirstName(),
                patient.getLastName(),
                buildFullName(
                        patient.getFirstName(),
                        patient.getLastName()
                ),
                patient.getMobile()
        );
    }

    private String buildFullName(
            String firstName,
            String lastName
    ) {

        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    private String maskAadhaar(String aadhaarNo) {
        return aadhaarNo == null ? null : "XXXXXXXX" + aadhaarNo.substring(8);
    }
}

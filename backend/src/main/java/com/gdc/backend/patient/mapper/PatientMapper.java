package com.gdc.backend.patient.mapper;

import org.springframework.stereotype.Component;

import com.gdc.backend.patient.dto.PatientCreateRequest;
import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.dto.PatientUpdateRequest;
import com.gdc.backend.patient.entity.Patient;

@Component
public class PatientMapper {

    /**
     * Converts a create request into a new Patient entity.
     *
     * System-managed fields such as:
     * id, patientId, createdAt, updatedAt and version
     * are intentionally not handled here.
     */
    public Patient toEntity(PatientCreateRequest request) {

        Patient patient = new Patient();

        patient.setFirstName(normalizeRequiredText(request.firstName()));
        patient.setLastName(normalizeOptionalText(request.lastName()));
        patient.setGender(request.gender());
        patient.setAge(request.age());
        patient.setMobile(normalizeMobile(request.mobile()));
        patient.setAddress(normalizeRequiredText(request.address()));
        patient.setReferredBy(request.referredBy());
        patient.setDoctorName(normalizeOptionalText(request.doctorName()));
        patient.setConsultationFee(request.consultationFee());
        patient.setRegistrationDate(request.registrationDate());
        patient.setActive(true);

        return patient;
    }

    /**
     * Applies editable fields from an update request
     * to an existing managed Patient entity.
     *
     * Identity, audit, lifecycle and version fields
     * are intentionally left untouched.
     */
    public void updateEntity(
            Patient patient,
            PatientUpdateRequest request
    ) {

        patient.setFirstName(normalizeRequiredText(request.firstName()));
        patient.setLastName(normalizeOptionalText(request.lastName()));
        patient.setGender(request.gender());
        patient.setAge(request.age());
        patient.setMobile(normalizeMobile(request.mobile()));
        patient.setAddress(normalizeRequiredText(request.address()));
        patient.setReferredBy(request.referredBy());
        patient.setDoctorName(normalizeOptionalText(request.doctorName()));
        patient.setConsultationFee(request.consultationFee());
        patient.setRegistrationDate(request.registrationDate());
    }

    /**
     * Converts the persistence entity into the external API response.
     */
    public PatientResponse toResponse(Patient patient) {

        return new PatientResponse(
                patient.getPatientId(),
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

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeMobile(String mobile) {
        return mobile.trim();
    }
}

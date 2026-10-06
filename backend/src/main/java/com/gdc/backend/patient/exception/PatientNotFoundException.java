package com.gdc.backend.patient.exception;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String patientId) {
        super("Patient not found: " + patientId);
    }
}
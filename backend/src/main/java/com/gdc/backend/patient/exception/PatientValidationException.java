package com.gdc.backend.patient.exception;

public class PatientValidationException extends RuntimeException {

    public PatientValidationException(String message) {
        super(message);
    }
}
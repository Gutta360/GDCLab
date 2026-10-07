package com.gdc.backend.patient.service;

import java.util.List;

import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;

public interface PatientService {

    PatientResponse getPatient(String patientId);

    List<PatientSummaryResponse> searchActivePatients(String query);
}

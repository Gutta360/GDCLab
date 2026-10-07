package com.gdc.backend.patient.service;

import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.entity.Patient;
import com.gdc.backend.patient.exception.PatientNotFoundException;
import com.gdc.backend.patient.exception.PatientValidationException;
import com.gdc.backend.patient.mapper.PatientMapper;
import com.gdc.backend.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    public PatientResponse getPatient(String patientId) {
        return patientMapper.toResponse(findActivePatient(patientId));
    }

    @Override
    public List<PatientSummaryResponse> searchActivePatients(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return List.of();
        }
        return patientRepository.searchActivePatients(normalizedQuery)
                .stream()
                .map(patientMapper::toSummaryResponse)
                .toList();
    }

    private Patient findActivePatient(String patientId) {
        String normalizedPatientId = normalizePatientId(patientId);
        return patientRepository.findByPatientIdAndActiveTrue(normalizedPatientId)
                .orElseThrow(() -> new PatientNotFoundException(normalizedPatientId));
    }

    private String normalizePatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new PatientValidationException("Patient ID is required");
        }
        return patientId.trim().toUpperCase();
    }
}

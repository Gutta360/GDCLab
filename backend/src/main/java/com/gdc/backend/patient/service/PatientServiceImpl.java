package com.gdc.backend.patient.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gdc.backend.patient.dto.PatientCreateRequest;
import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.dto.PatientUpdateRequest;
import com.gdc.backend.patient.entity.Patient;
import com.gdc.backend.patient.entity.ReferredBy;
import com.gdc.backend.patient.exception.DuplicatePatientException;
import com.gdc.backend.patient.exception.PatientNotFoundException;
import com.gdc.backend.patient.exception.PatientValidationException;
import com.gdc.backend.patient.mapper.PatientMapper;
import com.gdc.backend.patient.repository.PatientRepository;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

        private static final String PATIENT_ID_PREFIX = "P-";

        private final PatientRepository patientRepository;
        private final PatientMapper patientMapper;

        public PatientServiceImpl(
                        PatientRepository patientRepository,
                        PatientMapper patientMapper) {
                this.patientRepository = patientRepository;
                this.patientMapper = patientMapper;
        }

        @Override
        public PatientResponse createPatient(PatientCreateRequest request) {

                String firstName = normalizeRequiredText(request.firstName());
                String mobile = normalizeMobile(request.mobile());

                validateDoctorReferral(
                                request.referredBy(),
                                request.doctorName());

                validatePatientDoesNotAlreadyExist(
                                firstName,
                                mobile);

                Patient patient = patientMapper.toEntity(request);

                patient.setPatientId(generatePatientId());

                normalizeDoctorReferral(patient);

                Patient savedPatient = patientRepository.saveAndFlush(patient);
                return patientMapper.toResponse(savedPatient);
        }

        @Override
        @Transactional(readOnly = true)
        public PatientResponse getPatient(String patientId) {

                Patient patient = findActivePatient(patientId);

                return patientMapper.toResponse(patient);
        }

        @Override
        @Transactional(readOnly = true)
        public List<PatientResponse> getAllPatients() {

                return patientRepository.findAllByActiveTrue()
                                .stream()
                                .map(patientMapper::toResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<PatientSummaryResponse> searchActivePatients(String query) {

                String normalizedQuery =
                                query == null
                                                ? ""
                                                : query.trim();

                if (normalizedQuery.length() < 2) {
                        return List.of();
                }

                return patientRepository
                                .searchActivePatients(normalizedQuery)
                                .stream()
                                .map(patientMapper::toSummaryResponse)
                                .toList();
        }

        @Override
        public PatientResponse updatePatient(
                        String patientId,
                        PatientUpdateRequest request) {

                Patient patient = findActivePatient(patientId);

                String firstName = normalizeRequiredText(request.firstName());
                String mobile = normalizeMobile(request.mobile());

                validateDoctorReferral(
                                request.referredBy(),
                                request.doctorName());

                validateNoDuplicateForUpdate(
                                firstName,
                                mobile,
                                patientId);

                patientMapper.updateEntity(patient, request);

                normalizeDoctorReferral(patient);

                Patient updatedPatient = patientRepository.saveAndFlush(patient);
                return patientMapper.toResponse(updatedPatient);
        }

        @Override
        public void deletePatient(String patientId) {

                Patient patient = findActivePatient(patientId);

                patient.setActive(false);
                patient.setDeletedAt(Instant.now());

                patientRepository.save(patient);
        }

        private Patient findActivePatient(String patientId) {

                String normalizedPatientId = normalizePatientId(patientId);

                return patientRepository
                                .findByPatientIdAndActiveTrue(normalizedPatientId)
                                .orElseThrow(
                                                () -> new PatientNotFoundException(
                                                                normalizedPatientId));
        }

        private void validatePatientDoesNotAlreadyExist(
                        String firstName,
                        String mobile) {

                boolean exists = patientRepository.existsByFirstNameAndMobile(
                                firstName,
                                mobile);

                if (exists) {
                        throw new DuplicatePatientException(
                                        "A patient with the same first name and mobile number already exists");
                }
        }

        private void validateNoDuplicateForUpdate(
                        String firstName,
                        String mobile,
                        String patientId) {

                boolean exists = patientRepository
                                .existsByFirstNameAndMobileAndPatientIdNot(
                                                firstName,
                                                mobile,
                                                patientId);

                if (exists) {
                        throw new DuplicatePatientException(
                                        "Another patient with the same first name and mobile number already exists");
                }
        }

        private void validateDoctorReferral(
                        ReferredBy referredBy,
                        String doctorName) {

                if (referredBy == ReferredBy.DOCTOR
                                && (doctorName == null || doctorName.isBlank())) {

                        throw new PatientValidationException(
                                        "Doctor name is required when referred by a doctor");
                }
        }

        private void normalizeDoctorReferral(Patient patient) {

                if (patient.getReferredBy() != ReferredBy.DOCTOR) {
                        patient.setDoctorName(null);
                }
        }

        private String generatePatientId() {

                Long nextNumber = patientRepository.getNextPatientNumber();

                return PATIENT_ID_PREFIX + "%05d".formatted(nextNumber);
        }

        private String normalizePatientId(String patientId) {

                if (patientId == null || patientId.isBlank()) {
                        throw new PatientValidationException(
                                        "Patient ID is required");
                }

                return patientId.trim().toUpperCase();
        }

        private String normalizeRequiredText(String value) {
                return value.trim();
        }

        private String normalizeMobile(String mobile) {
                return mobile.trim();
        }
}

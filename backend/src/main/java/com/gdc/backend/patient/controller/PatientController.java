package com.gdc.backend.patient.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gdc.backend.patient.dto.PatientCreateRequest;
import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.dto.PatientUpdateRequest;
import com.gdc.backend.patient.service.PatientService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientCreateRequest request
    ) {

        PatientResponse response =
                patientService.createPatient(request);

        URI location = URI.create(
                "/api/v1/patients/" + response.patientId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId
    ) {

        PatientResponse response =
                patientService.getPatient(patientId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients() {

        List<PatientResponse> response =
                patientService.getAllPatients();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PatientSummaryResponse>> searchPatients(
            @RequestParam String query
    ) {

        List<PatientSummaryResponse> response =
                patientService.searchActivePatients(query);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String patientId,
            @Valid @RequestBody PatientUpdateRequest request
    ) {

        PatientResponse response =
                patientService.updatePatient(
                        patientId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String patientId
    ) {

        patientService.deletePatient(patientId);

        return ResponseEntity.noContent().build();
    }
}

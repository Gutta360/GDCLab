package com.gdc.backend.patient.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gdc.backend.patient.dto.PatientResponse;
import com.gdc.backend.patient.dto.PatientSummaryResponse;
import com.gdc.backend.patient.service.PatientService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId
    ) {

        PatientResponse response =
                patientService.getPatient(patientId);

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

}

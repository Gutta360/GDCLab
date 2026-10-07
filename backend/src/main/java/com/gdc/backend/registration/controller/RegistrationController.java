package com.gdc.backend.registration.controller;

import com.gdc.backend.common.dto.PageResponse;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/registrations")
public class RegistrationController {
    private final RegistrationService service;
    public RegistrationController(RegistrationService service){this.service=service;}
    @PostMapping public ResponseEntity<RegistrationResponse> create(@Valid @RequestBody RegistrationCreateRequest request){
        RegistrationResponse response=service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/registrations/"+response.registrationNo())).body(response);
    }
    @GetMapping("/{registrationNo}") public RegistrationResponse get(@PathVariable String registrationNo){return service.get(registrationNo);}
    @GetMapping("/{registrationNo}/bill") public BillResponse bill(@PathVariable String registrationNo){return service.bill(registrationNo);}
    @PutMapping("/{registrationNo}/tests/{testCode}/status")
    public RegistrationTestResponse status(@PathVariable String registrationNo,@PathVariable String testCode,
            @Valid @RequestBody TestStatusUpdateRequest request){return service.updateTestStatus(registrationNo,testCode,request);}
    @GetMapping public PageResponse<RegistrationSearchRow> search(
            @RequestParam(required=false)String registrationNo,@RequestParam(required=false)String billNo,
            @RequestParam(required=false)String patientId,@RequestParam(required=false)String patientName,
            @RequestParam(required=false)String mobile,@RequestParam(required=false)Long branchId,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fromDate,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate toDate,
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return service.search(registrationNo,billNo,patientId,patientName,mobile,branchId,fromDate,toDate,page,size);
    }
}

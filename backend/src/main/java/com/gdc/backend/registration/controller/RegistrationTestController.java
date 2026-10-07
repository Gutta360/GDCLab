package com.gdc.backend.registration.controller;

import com.gdc.backend.common.dto.PagedReportResponse;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.TestStatus;
import com.gdc.backend.registration.service.RegistrationTestSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/registration-tests")
public class RegistrationTestController {
    private final RegistrationTestSearchService service;
    public RegistrationTestController(RegistrationTestSearchService service){this.service=service;}
    @GetMapping public PagedReportResponse<RegistrationTestSearchRow,TestSearchSummary> search(
            @RequestParam(required=false)String testCode,@RequestParam(required=false)String testName,
            @RequestParam(required=false)String category,@RequestParam(required=false)String registrationNo,
            @RequestParam(required=false)String patientName,@RequestParam(required=false)Long branchId,
            @RequestParam(required=false)TestStatus status,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fromDate,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate toDate,
            @RequestParam(defaultValue="REGISTERED")TestDateMode dateMode,
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return service.search(testCode,testName,category,registrationNo,patientName,branchId,status,fromDate,toDate,dateMode,page,size);
    }
}

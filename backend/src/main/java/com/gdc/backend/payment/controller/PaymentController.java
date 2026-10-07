package com.gdc.backend.payment.controller;

import com.gdc.backend.common.dto.PagedReportResponse;
import com.gdc.backend.payment.dto.*;
import com.gdc.backend.payment.entity.PaymentMode;
import com.gdc.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service){this.service=service;}
    @PostMapping("/registrations/{registrationNo}/payments")
    public ResponseEntity<PaymentResponse> create(@PathVariable String registrationNo,@Valid @RequestBody PaymentCreateRequest request){
        PaymentResponse response=service.create(registrationNo,request);
        return ResponseEntity.created(URI.create("/api/v1/payments/"+response.paymentNo())).body(response);
    }
    @GetMapping("/payments") public PagedReportResponse<PaymentSearchRow,PaymentSearchSummary> search(
            @RequestParam(required=false)String paymentNo,@RequestParam(required=false)String billNo,
            @RequestParam(required=false)String registrationNo,@RequestParam(required=false)String patientName,
            @RequestParam(required=false)String mobile,@RequestParam(required=false)PaymentMode paymentMode,
            @RequestParam(required=false)Long branchId,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fromDate,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate toDate,
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return service.search(paymentNo,billNo,registrationNo,patientName,mobile,paymentMode,branchId,fromDate,toDate,page,size);
    }
}

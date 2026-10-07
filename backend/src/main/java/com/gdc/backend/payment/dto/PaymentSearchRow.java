package com.gdc.backend.payment.dto;

import com.gdc.backend.payment.entity.PaymentMode;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSearchRow(String paymentNo, Instant paymentDatetime, String registrationNo,
        String billNo, String patientName, String mobile, PaymentMode paymentMode,
        BigDecimal amount, String receivedBy, String branchCode, String branchName) {}

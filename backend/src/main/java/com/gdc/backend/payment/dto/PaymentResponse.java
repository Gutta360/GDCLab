package com.gdc.backend.payment.dto;

import com.gdc.backend.payment.entity.PaymentMode;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String paymentNo, BigDecimal amount, PaymentMode paymentMode,
                              Instant paymentDatetime, String referenceNo, String remarks, String receivedBy) {}

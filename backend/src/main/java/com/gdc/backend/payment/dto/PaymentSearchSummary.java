package com.gdc.backend.payment.dto;

import java.math.BigDecimal;

public record PaymentSearchSummary(long paymentCount, BigDecimal cashTotal, BigDecimal upiTotal,
        BigDecimal cardTotal, BigDecimal otherTotal, BigDecimal grandTotal) {}

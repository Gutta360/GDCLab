package com.gdc.backend.common.money;

import com.gdc.backend.common.exception.BusinessValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private Money() {}

    public static BigDecimal amount(BigDecimal value, String field) {
        if (value == null) return ZERO;
        if (value.signum() < 0) throw new BusinessValidationException(field + " cannot be negative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentage(BigDecimal value, String field) {
        BigDecimal result = amount(value, field);
        if (result.compareTo(HUNDRED) > 0) {
            throw new BusinessValidationException(field + " must be between 0 and 100");
        }
        return result;
    }

    public static BigDecimal discount(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}

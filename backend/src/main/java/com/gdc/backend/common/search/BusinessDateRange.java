package com.gdc.backend.common.search;

import com.gdc.backend.common.exception.BusinessValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record BusinessDateRange(Instant fromInclusive, Instant toExclusive) {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    public static BusinessDateRange from(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return new BusinessDateRange(null, null);
        }
        if (fromDate == null || toDate == null) {
            throw new BusinessValidationException("Both fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new BusinessValidationException("toDate cannot be before fromDate");
        }
        return new BusinessDateRange(
                fromDate.atStartOfDay(BUSINESS_ZONE).toInstant(),
                toDate.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
        );
    }
}

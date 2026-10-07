package com.gdc.backend.common.search;

import com.gdc.backend.common.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class BusinessDateRangeTest {
    @Test void convertsIndiaDatesToInclusiveExclusiveInstantsAcrossYearEnd() {
        var range=BusinessDateRange.from(LocalDate.of(2026,12,31),LocalDate.of(2026,12,31));
        assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-12-30T18:30:00Z"));
        assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-12-31T18:30:00Z"));
    }
    @Test void acceptsNoDateRange() { assertThat(BusinessDateRange.from(null,null).fromInclusive()).isNull(); }
    @Test void rejectsPartialOrReversedRanges() {
        assertThatThrownBy(()->BusinessDateRange.from(LocalDate.now(),null)).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(()->BusinessDateRange.from(LocalDate.of(2026,2,2),LocalDate.of(2026,2,1))).isInstanceOf(BusinessValidationException.class);
    }
}

package com.gdc.backend.registration.service;

import com.gdc.backend.registration.entity.RegistrationStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class RegistrationFinancialStatusTest {
    @Test void derivesOpenPartialAndPaidStatuses() {
        assertThat(RegistrationService.status(new BigDecimal("0.00"),new BigDecimal("100.00"))).isEqualTo(RegistrationStatus.OPEN);
        assertThat(RegistrationService.status(new BigDecimal("40.00"),new BigDecimal("60.00"))).isEqualTo(RegistrationStatus.PARTIALLY_PAID);
        assertThat(RegistrationService.status(new BigDecimal("0.00"),new BigDecimal("0.00"))).isEqualTo(RegistrationStatus.PAID);
    }
}

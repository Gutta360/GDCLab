package com.gdc.backend.common.money;

import com.gdc.backend.common.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test void roundsHalfUpAtTwoDecimalPlaces() {
        assertThat(Money.discount(new BigDecimal("0.05"), new BigDecimal("10.00")))
                .isEqualByComparingTo("0.01");
    }
    @Test void appliesTestAndBillDiscountsExactlyOnce() {
        BigDecimal testTotal = new BigDecimal("1000.00").subtract(Money.discount(new BigDecimal("1000.00"), new BigDecimal("10")));
        BigDecimal finalTotal = testTotal.subtract(Money.discount(testTotal, new BigDecimal("5")));
        assertThat(finalTotal).isEqualByComparingTo("855.00");
    }
    @Test void rejectsInvalidPercentagesAndNegativeAmounts() {
        assertThatThrownBy(() -> Money.percentage(new BigDecimal("100.01"), "Discount")).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> Money.amount(new BigDecimal("-0.01"), "Amount")).isInstanceOf(BusinessValidationException.class);
    }
}

package com.gdc.backend.registration.service;

import com.gdc.backend.registration.entity.TestStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TestStatusTransitionsTest {
    @Test void allowsApprovedTransitions() {
        assertThat(TestStatusTransitions.allows(TestStatus.REGISTERED,TestStatus.IN_PROGRESS)).isTrue();
        assertThat(TestStatusTransitions.allows(TestStatus.REGISTERED,TestStatus.COMPLETED)).isTrue();
        assertThat(TestStatusTransitions.allows(TestStatus.REGISTERED,TestStatus.CANCELLED)).isTrue();
        assertThat(TestStatusTransitions.allows(TestStatus.IN_PROGRESS,TestStatus.COMPLETED)).isTrue();
        assertThat(TestStatusTransitions.allows(TestStatus.IN_PROGRESS,TestStatus.CANCELLED)).isTrue();
    }
    @Test void completedAndCancelledAreTerminal() {
        for(TestStatus target:TestStatus.values()) {
            assertThat(TestStatusTransitions.allows(TestStatus.COMPLETED,target)).isFalse();
            assertThat(TestStatusTransitions.allows(TestStatus.CANCELLED,target)).isFalse();
        }
    }
}

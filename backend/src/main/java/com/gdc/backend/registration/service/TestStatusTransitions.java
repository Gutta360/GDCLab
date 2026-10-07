package com.gdc.backend.registration.service;

import com.gdc.backend.registration.entity.TestStatus;

public final class TestStatusTransitions {
    private TestStatusTransitions() {}

    public static boolean allows(TestStatus from, TestStatus to) {
        return (from == TestStatus.REGISTERED
                && (to == TestStatus.IN_PROGRESS || to == TestStatus.COMPLETED || to == TestStatus.CANCELLED))
                || (from == TestStatus.IN_PROGRESS
                && (to == TestStatus.COMPLETED || to == TestStatus.CANCELLED));
    }
}

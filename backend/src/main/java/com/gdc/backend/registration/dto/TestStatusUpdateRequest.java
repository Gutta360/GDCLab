package com.gdc.backend.registration.dto;

import com.gdc.backend.registration.entity.TestStatus;
import jakarta.validation.constraints.NotNull;

public record TestStatusUpdateRequest(@NotNull TestStatus status, @NotNull Long expectedVersion) {}

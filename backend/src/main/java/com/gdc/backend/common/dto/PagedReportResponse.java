package com.gdc.backend.common.dto;

public record PagedReportResponse<T, S>(PageResponse<T> page, S summary) {}

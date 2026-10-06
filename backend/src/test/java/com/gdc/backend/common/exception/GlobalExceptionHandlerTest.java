package com.gdc.backend.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void lockFailureReturnsConflictWithRefreshMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleConcurrency(new CannotAcquireLockException("deadlock"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Please refresh and try again");
    }
}

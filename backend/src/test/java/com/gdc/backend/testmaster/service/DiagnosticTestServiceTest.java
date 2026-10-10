package com.gdc.backend.testmaster.service;

import com.gdc.backend.testmaster.repository.DiagnosticTestRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticTestServiceTest {
    @Test void replacesMissingFiltersWithTypedValues() {
        AtomicReference<Object[]> arguments = new AtomicReference<>();
        DiagnosticTestRepository tests = (DiagnosticTestRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{DiagnosticTestRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("search")) {
                        arguments.set(args);
                        return List.of();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        DiagnosticTestService service = new DiagnosticTestService(tests, null);

        assertThat(service.search(null, null, true)).isEmpty();
        assertThat(arguments.get()).containsExactly("", "", true, true);
    }
}

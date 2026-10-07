package com.gdc.backend.testmaster.service;

import com.gdc.backend.common.exception.*;
import com.gdc.backend.testmaster.dto.*;
import com.gdc.backend.testmaster.entity.DiagnosticTest;
import com.gdc.backend.testmaster.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DiagnosticTestService {
    private final DiagnosticTestRepository tests;
    private final TestCategoryRepository categories;
    public DiagnosticTestService(DiagnosticTestRepository tests, TestCategoryRepository categories) {
        this.tests = tests; this.categories = categories;
    }
    public List<TestCategoryResponse> categories() {
        return categories.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(c -> new TestCategoryResponse(c.getId(), c.getCategoryCode(), c.getCategoryName(), c.getDisplayOrder())).toList();
    }
    public List<DiagnosticTestResponse> search(String query, String category, Boolean active) {
        String q = normalize(query); String c = normalize(category);
        return tests.search(q, c, active).stream().map(this::response).toList();
    }
    public DiagnosticTest get(String code) {
        return tests.findByTestCodeWithCategory(code == null ? "" : code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic test not found: " + code));
    }
    public DiagnosticTest requireActive(String code) {
        DiagnosticTest test = get(code);
        if (!test.isActive() || !test.getCategory().isActive())
            throw new BusinessValidationException("Diagnostic test is inactive: " + test.getTestCode());
        return test;
    }
    public DiagnosticTestResponse response(DiagnosticTest t) {
        return new DiagnosticTestResponse(t.getTestCode(), t.getTestName(), t.getCategory().getCategoryCode(),
                t.getCategory().getCategoryName(), t.getCurrentPrice(), t.getDisplayOrder(), t.isActive());
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

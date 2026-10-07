package com.gdc.backend.testmaster.controller;

import com.gdc.backend.testmaster.dto.*;
import com.gdc.backend.testmaster.service.DiagnosticTestService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1")
public class DiagnosticTestController {
    private final DiagnosticTestService service;
    public DiagnosticTestController(DiagnosticTestService service) { this.service = service; }
    @GetMapping("/test-categories") public List<TestCategoryResponse> categories() { return service.categories(); }
    @GetMapping("/tests") public List<DiagnosticTestResponse> tests(@RequestParam(required=false) String query,
            @RequestParam(required=false) String category, @RequestParam(required=false) Boolean active) {
        return service.search(query, category, active);
    }
    @GetMapping("/tests/{testCode}") public DiagnosticTestResponse test(@PathVariable String testCode) {
        return service.response(service.get(testCode));
    }
}

package com.gdc.backend.branch.controller;

import com.gdc.backend.branch.dto.BranchResponse;
import com.gdc.backend.branch.service.BranchService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/branches")
public class BranchController {
    private final BranchService service;
    public BranchController(BranchService service) { this.service = service; }
    @GetMapping public List<BranchResponse> getActive() { return service.getActiveBranches(); }
}

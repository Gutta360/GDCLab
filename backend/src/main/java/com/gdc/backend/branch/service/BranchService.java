package com.gdc.backend.branch.service;

import com.gdc.backend.branch.dto.BranchResponse;
import com.gdc.backend.branch.entity.Branch;
import com.gdc.backend.branch.repository.BranchRepository;
import com.gdc.backend.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BranchService {
    private final BranchRepository repository;
    public BranchService(BranchRepository repository) { this.repository = repository; }
    public List<BranchResponse> getActiveBranches() {
        return repository.findAllByActiveTrueOrderByBranchNameAsc().stream()
                .map(b -> new BranchResponse(b.getId(), b.getBranchCode(), b.getBranchName(), b.getAddress(), b.getPhone(), b.getMobile()))
                .toList();
    }
    public Branch requireActive(Long id) {
        Branch branch = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!branch.isActive()) throw new com.gdc.backend.common.exception.BusinessValidationException("Branch is inactive");
        return branch;
    }
}

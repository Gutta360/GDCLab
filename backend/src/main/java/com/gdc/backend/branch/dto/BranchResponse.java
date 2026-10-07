package com.gdc.backend.branch.dto;

public record BranchResponse(Long id, String branchCode, String branchName, String address,
                             String phone, String mobile) {}

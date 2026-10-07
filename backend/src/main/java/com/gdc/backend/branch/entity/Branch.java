package com.gdc.backend.branch.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "branches")
public class Branch extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "branch_code", nullable = false, unique = true, length = 20) private String branchCode;
    @Column(name = "branch_name", nullable = false, length = 150) private String branchName;
    @Column(nullable = false, length = 500) private String address;
    @Column(length = 20) private String phone;
    @Column(length = 20) private String mobile;
    @Column(name = "is_active", nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getBranchCode() { return branchCode; }
    public String getBranchName() { return branchName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getMobile() { return mobile; }
    public boolean isActive() { return active; }
}

package com.gdc.backend.referral.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "referral_sources")
public class ReferralSource extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "referral_code", nullable = false, unique = true, length = 20) private String referralCode;
    @Enumerated(EnumType.STRING) @Column(name = "referral_type", nullable = false, length = 10) private ReferralType referralType;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 100) private String qualification;
    @Column(length = 20) private String mobile;
    @Column(name = "is_active", nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getReferralCode() { return referralCode; }
    public ReferralType getReferralType() { return referralType; }
    public String getName() { return name; }
    public String getQualification() { return qualification; }
    public String getMobile() { return mobile; }
    public boolean isActive() { return active; }
}

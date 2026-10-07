package com.gdc.backend.referral.repository;

import com.gdc.backend.referral.entity.ReferralSource;
import com.gdc.backend.referral.entity.ReferralType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReferralSourceRepository extends JpaRepository<ReferralSource, Long> {
    List<ReferralSource> findAllByActiveTrueOrderByReferralTypeAscNameAsc();
    List<ReferralSource> findAllByActiveTrueAndReferralTypeOrderByNameAsc(ReferralType type);
}

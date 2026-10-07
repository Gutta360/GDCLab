package com.gdc.backend.referral.service;

import com.gdc.backend.common.exception.BusinessValidationException;
import com.gdc.backend.common.exception.ResourceNotFoundException;
import com.gdc.backend.referral.dto.ReferralSourceResponse;
import com.gdc.backend.referral.entity.*;
import com.gdc.backend.referral.repository.ReferralSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferralService {
    private final ReferralSourceRepository repository;
    public ReferralService(ReferralSourceRepository repository) { this.repository = repository; }
    public List<ReferralSourceResponse> getActive(ReferralType type) {
        var values = type == null ? repository.findAllByActiveTrueOrderByReferralTypeAscNameAsc()
                : repository.findAllByActiveTrueAndReferralTypeOrderByNameAsc(type);
        return values.stream().map(r -> new ReferralSourceResponse(r.getId(), r.getReferralCode(), r.getReferralType(),
                r.getName(), r.getQualification(), r.getMobile())).toList();
    }
    public ReferralSource requireActiveType(Long id, ReferralType expected) {
        if (id == null) return null;
        ReferralSource source = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(expected + " referral source not found"));
        if (!source.isActive()) throw new BusinessValidationException(expected + " referral source is inactive");
        if (source.getReferralType() != expected) throw new BusinessValidationException("Referral source is not a " + expected);
        return source;
    }
}

package com.gdc.backend.referral.dto;

import com.gdc.backend.referral.entity.ReferralType;

public record ReferralSourceResponse(Long id, String referralCode, ReferralType referralType,
                                     String name, String qualification, String mobile) {}

package com.gdc.backend.referral.controller;

import com.gdc.backend.referral.dto.ReferralSourceResponse;
import com.gdc.backend.referral.entity.ReferralType;
import com.gdc.backend.referral.service.ReferralService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/referral-sources")
public class ReferralController {
    private final ReferralService service;
    public ReferralController(ReferralService service) { this.service = service; }
    @GetMapping public List<ReferralSourceResponse> getActive(@RequestParam(required = false) ReferralType type) {
        return service.getActive(type);
    }
}

package com.mittechkernel.backend.modules.gamification;

import com.mittechkernel.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/xp")
@PreAuthorize("isAuthenticated()")
public class XpController {
    private final XpService xpService;

    public XpController(XpService xpService) {
        this.xpService = xpService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<XpSummaryResponse>> getMyXp(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(xpService.getMySummary(), request));
    }
}

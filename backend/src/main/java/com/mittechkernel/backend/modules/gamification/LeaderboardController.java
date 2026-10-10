package com.mittechkernel.backend.modules.gamification;

import com.mittechkernel.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
@PreAuthorize("isAuthenticated()")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LeaderboardPage>> overall(
            @RequestParam(defaultValue = "TECHNICAL") String program,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaderboardService.getOverall(program, page, size), request));
    }

    @GetMapping("/domains/{domainId}")
    public ResponseEntity<ApiResponse<LeaderboardPage>> domain(
            @PathVariable Long domainId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaderboardService.getDomain(domainId, page, size), request));
    }
}

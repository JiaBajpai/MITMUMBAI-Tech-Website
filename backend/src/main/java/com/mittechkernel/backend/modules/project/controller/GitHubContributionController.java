package com.mittechkernel.backend.modules.project.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.project.dto.GitHubContributionResponse;
import com.mittechkernel.backend.modules.project.service.GitHubContributionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/github/contributions")
public class GitHubContributionController {

    private final GitHubContributionService contributionService;

    public GitHubContributionController(GitHubContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<GitHubContributionResponse>>> getContributions(
            @PathVariable Long projectId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contributionService.syncAndList(projectId), request));
    }

    @PostMapping("/{contributionId}/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GitHubContributionResponse>> verifyContribution(
            @PathVariable Long projectId, @PathVariable Long contributionId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contributionService.verify(projectId, contributionId), request));
    }
}

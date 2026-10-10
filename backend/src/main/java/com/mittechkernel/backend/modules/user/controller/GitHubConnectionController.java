package com.mittechkernel.backend.modules.user.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.user.dto.GitHubCallbackResponse;
import com.mittechkernel.backend.modules.user.dto.GitHubConnectionResponse;
import com.mittechkernel.backend.modules.user.dto.GitHubConnectionStatusResponse;
import com.mittechkernel.backend.modules.user.service.GitHubConnectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GitHubConnectionController {

    private final GitHubConnectionService gitHubConnectionService;

    public GitHubConnectionController(GitHubConnectionService gitHubConnectionService) {
        this.gitHubConnectionService = gitHubConnectionService;
    }

    @PostMapping("/me/github/connect")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GitHubConnectionResponse>> connect(HttpServletRequest request,
                                                                        HttpSession session) {
        GitHubConnectionResponse response = gitHubConnectionService.initiateConnection(session);
        return ResponseEntity.ok(ApiResponse.success(response, request));
    }

    @GetMapping("/me/github/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GitHubConnectionStatusResponse>> status(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gitHubConnectionService.getStatus(), request));
    }

    @DeleteMapping("/me/github/connect")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disconnect() {
        gitHubConnectionService.disconnectCurrentUser();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/github/oauth/callback")
    public ResponseEntity<ApiResponse<GitHubCallbackResponse>> callback(@RequestParam(required = false) String code,
                                                                      @RequestParam(required = false) String state,
                                                                      @RequestParam(required = false) String error,
                                                                      HttpServletRequest request,
                                                                      HttpSession session) {
        GitHubCallbackResponse response = gitHubConnectionService.validateCallback(code, state, error, session);
        return ResponseEntity.ok(ApiResponse.success(response, request));
    }
}

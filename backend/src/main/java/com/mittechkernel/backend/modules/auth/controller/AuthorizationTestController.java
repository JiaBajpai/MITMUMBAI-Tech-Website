package com.mittechkernel.backend.modules.auth.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class AuthorizationTestController {

    private final DomainAuthorizationService domainAuthorizationService;

    public AuthorizationTestController(DomainAuthorizationService domainAuthorizationService) {
        this.domainAuthorizationService = domainAuthorizationService;
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> student(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("student-ok", request));
    }

    @GetMapping("/core-member")
    @PreAuthorize("hasRole('CORE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> coreMember(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("core-member-ok", request));
    }

    @GetMapping("/faculty")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<String>> faculty(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("faculty-ok", request));
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> system(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("system-ok", request));
    }

    @GetMapping("/domain")
    @PreAuthorize("hasRole('DOMAIN_LEAD') and @domainAuthorizationService.hasCurrentUserDomainAccess(#domainId)")
    public ResponseEntity<ApiResponse<String>> domain(HttpServletRequest request, @RequestParam Long domainId) {
        return ResponseEntity.ok(ApiResponse.success("domain-access-ok:" + domainId, request));
    }
}

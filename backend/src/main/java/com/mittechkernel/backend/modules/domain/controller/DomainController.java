package com.mittechkernel.backend.modules.domain.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.domain.dto.DomainResponse;
import com.mittechkernel.backend.modules.domain.service.DomainService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/domains")
    public ResponseEntity<ApiResponse<List<DomainResponse>>> listDomains(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(domainService.getAllDomains(), request));
    }

    @GetMapping("/domains/{id}")
    @PreAuthorize("hasRole('DOMAIN_LEAD') and @domainAuthorizationService.hasCurrentUserDomainAccess(#domainId)")
    public ResponseEntity<ApiResponse<DomainResponse>> getDomain(@PathVariable("id") Long domainId,
                                                                HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(domainService.getDomain(domainId), request));
    }
}

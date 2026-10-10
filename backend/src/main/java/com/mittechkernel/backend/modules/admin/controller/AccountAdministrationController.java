package com.mittechkernel.backend.modules.admin.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.admin.dto.AccountResponse;
import com.mittechkernel.backend.modules.admin.dto.CreateAccountRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateAccountRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateRolesRequest;
import com.mittechkernel.backend.modules.admin.service.AccountAdministrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CORE_MEMBER')")
public class AccountAdministrationController {
    private final AccountAdministrationService service;

    public AccountAdministrationController(AccountAdministrationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> list(@RequestParam(required = false) String q, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.list(q), request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody CreateAccountRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(body), request, HttpStatus.CREATED.value()));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<AccountResponse>> update(@PathVariable long userId, @Valid @RequestBody UpdateAccountRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(userId, body), request));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<AccountResponse>> setRoles(@PathVariable long userId, @Valid @RequestBody UpdateRolesRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.setRoles(userId, body), request));
    }

    @PutMapping("/{userId}/domains/{domainId}")
    public ResponseEntity<ApiResponse<AccountResponse>> assignDomain(@PathVariable long userId, @PathVariable long domainId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.assignDomainLead(userId, domainId), request));
    }

    @DeleteMapping("/{userId}/domains/{domainId}")
    public ResponseEntity<ApiResponse<AccountResponse>> removeDomain(@PathVariable long userId, @PathVariable long domainId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.removeDomainLead(userId, domainId), request));
    }

    @PostMapping("/{userId}/password-reset")
    public ResponseEntity<ApiResponse<Void>> issuePasswordReset(@PathVariable long userId, HttpServletRequest request) {
        service.issuePasswordReset(userId);
        return ResponseEntity.ok(ApiResponse.success(null, request));
    }
}

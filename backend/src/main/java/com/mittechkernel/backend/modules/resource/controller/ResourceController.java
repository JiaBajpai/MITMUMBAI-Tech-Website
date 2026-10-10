package com.mittechkernel.backend.modules.resource.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.resource.dto.ResourceResponse;
import com.mittechkernel.backend.modules.resource.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listResources(
            @RequestParam(name = "domainId", required = false) Long domainId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "difficulty", required = false) String difficulty,
            @RequestParam(name = "topic", required = false) String topic,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.getResources(domainId, type, difficulty, topic),
                request
        ));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(@PathVariable Long id,
                                                                    HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(resourceService.getResource(id), request));
    }
}

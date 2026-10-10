package com.mittechkernel.backend.modules.project.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.project.dto.ProjectMemberResponse;
import com.mittechkernel.backend.modules.project.dto.ProjectRequest;
import com.mittechkernel.backend.modules.project.dto.ProjectResponse;
import com.mittechkernel.backend.modules.project.dto.ProjectReviewRequest;
import com.mittechkernel.backend.modules.project.service.ProjectService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(
            @RequestParam(name = "domainId", required = false) Long domainId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "program", required = false) String program,
            @RequestParam(name = "q", required = false) String query,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.getProjects(domainId, status, program, query),
                request
        ));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable Long id,
                                                                  HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id), request));
    }

    @GetMapping("/projects/{id}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(@PathVariable Long id,
                                                                                     HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getMembers(id), request));
    }

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, httpServletRequest, HttpStatus.CREATED.value()));
    }

    @PatchMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(@PathVariable Long id,
                                                                    @RequestBody ProjectRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(id, request), httpServletRequest));
    }

    @PostMapping("/projects/{id}/review")
    public ResponseEntity<ApiResponse<ProjectResponse>> reviewProject(@PathVariable Long id,
                                                                    @Valid @RequestBody ProjectReviewRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success(projectService.reviewProject(id, request), httpServletRequest));
    }

    @PostMapping("/projects/{id}/join")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> joinProject(@PathVariable Long id,
                                                                        HttpServletRequest request) {
        ProjectMemberResponse response = projectService.requestMembership(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, request, HttpStatus.CREATED.value()));
    }

    @PostMapping("/projects/{id}/members/{userId}/approve")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> approveMembership(@PathVariable Long id,
                                                                              @PathVariable Long userId,
                                                                              HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.approveMembership(id, userId), request));
    }

    @PostMapping("/projects/{id}/members/{userId}/reject")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> rejectMembership(@PathVariable Long id,
                                                                             @PathVariable Long userId,
                                                                             HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.rejectMembership(id, userId), request));
    }

    @DeleteMapping("/projects/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id,
                                            @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{id}/leave")
    public ResponseEntity<Void> leaveProject(@PathVariable Long id) {
        projectService.leaveProject(id);
        return ResponseEntity.noContent().build();
    }
}

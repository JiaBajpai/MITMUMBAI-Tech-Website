package com.mittechkernel.backend.modules.project.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.project.dto.GitHubRepositoryRequest;
import com.mittechkernel.backend.modules.project.dto.GitHubRepositoryResponse;
import com.mittechkernel.backend.modules.project.service.ProjectGitHubRepositoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class ProjectGitHubRepositoryController {

    private final ProjectGitHubRepositoryService projectGitHubRepositoryService;

    public ProjectGitHubRepositoryController(ProjectGitHubRepositoryService projectGitHubRepositoryService) {
        this.projectGitHubRepositoryService = projectGitHubRepositoryService;
    }

    @GetMapping("/me/github/repositories")
    public ResponseEntity<ApiResponse<List<GitHubRepositoryResponse>>> listAccessibleRepositories(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectGitHubRepositoryService.listAccessibleRepositories(), request));
    }

    @PostMapping("/projects/{id}/github/repository")
    public ResponseEntity<ApiResponse<GitHubRepositoryResponse>> linkRepository(@PathVariable("id") Long projectId,
                                                                              @Valid @RequestBody GitHubRepositoryRequest request,
                                                                              HttpServletRequest httpServletRequest) {
        GitHubRepositoryResponse response = projectGitHubRepositoryService.linkRepository(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, httpServletRequest, HttpStatus.CREATED.value()));
    }

    @GetMapping("/projects/{id}/github/repository")
    public ResponseEntity<ApiResponse<GitHubRepositoryResponse>> getLinkedRepository(@PathVariable("id") Long projectId,
                                                                                   HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectGitHubRepositoryService.getLinkedRepository(projectId), request));
    }

    @DeleteMapping("/projects/{id}/github/repository")
    public ResponseEntity<Void> unlinkRepository(@PathVariable("id") Long projectId) {
        projectGitHubRepositoryService.unlinkRepository(projectId);
        return ResponseEntity.noContent().build();
    }
}

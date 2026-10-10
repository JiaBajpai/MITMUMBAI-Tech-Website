package com.mittechkernel.backend.modules.project.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubRepository;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.project.dto.GitHubRepositoryRequest;
import com.mittechkernel.backend.modules.project.dto.GitHubRepositoryResponse;
import com.mittechkernel.backend.modules.project.entity.Project;
import com.mittechkernel.backend.modules.project.entity.ProjectGitHubRepository;
import com.mittechkernel.backend.modules.project.entity.ProjectMember;
import com.mittechkernel.backend.modules.project.repository.ProjectGitHubRepositoryRepository;
import com.mittechkernel.backend.modules.project.repository.ProjectMemberRepository;
import com.mittechkernel.backend.modules.project.repository.ProjectRepository;
import com.mittechkernel.backend.modules.project.repository.GitHubContributionRepository;
import com.mittechkernel.backend.modules.user.service.GitHubConnectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProjectGitHubRepositoryService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository;
    private final GitHubContributionRepository contributionRepository;
    private final CurrentUserService currentUserService;
    private final GitHubConnectionService gitHubConnectionService;
    private final GitHubClient gitHubClient;

    public ProjectGitHubRepositoryService(ProjectRepository projectRepository,
                                         ProjectMemberRepository projectMemberRepository,
                                         ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository,
                                         GitHubContributionRepository contributionRepository,
                                         CurrentUserService currentUserService,
                                         GitHubConnectionService gitHubConnectionService,
                                         GitHubClient gitHubClient) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectGitHubRepositoryRepository = projectGitHubRepositoryRepository;
        this.contributionRepository = contributionRepository;
        this.currentUserService = currentUserService;
        this.gitHubConnectionService = gitHubConnectionService;
        this.gitHubClient = gitHubClient;
    }

    @Transactional(readOnly = true)
    public List<GitHubRepositoryResponse> listAccessibleRepositories() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        String accessToken = gitHubConnectionService.requireAccessTokenForUser(currentUser.id());
        return gitHubClient.listAccessibleRepositories(accessToken)
                .stream()
                .map(repository -> new GitHubRepositoryResponse(
                        null,
                        null,
                        repository.id(),
                        repository.ownerLogin(),
                        repository.name(),
                        repository.fullName(),
                        repository.htmlUrl(),
                        repository.visibility(),
                        null,
                        null
                ))
                .toList();
    }

    @Transactional
    public GitHubRepositoryResponse linkRepository(Long projectId, GitHubRepositoryRequest request) {
        if (request == null || request.githubRepoId() == null) {
            throw new BadRequestException("A GitHub repository ID is required");
        }

        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!isProjectOwner(currentUser, project)) {
            throw new ForbiddenException("Only the project owner may link a GitHub repository");
        }

        if (projectGitHubRepositoryRepository.findByProjectId(projectId).isPresent()) {
            throw new BadRequestException("This project already has a linked GitHub repository");
        }

        if (projectGitHubRepositoryRepository.findByGithubRepoId(request.githubRepoId()).isPresent()) {
            throw new BadRequestException("This GitHub repository is already linked to another project");
        }

        String accessToken = gitHubConnectionService.requireAccessTokenForUser(currentUser.id());
        GitHubRepository repository = gitHubClient.getRepository(accessToken, request.githubRepoId());
        if (repository == null) {
            throw new BadRequestException("The selected GitHub repository is not accessible to the connected GitHub account");
        }

        ProjectGitHubRepository linked = new ProjectGitHubRepository();
        linked.setProjectId(projectId);
        linked.setGithubRepoId(repository.id());
        linked.setOwnerLogin(repository.ownerLogin());
        linked.setRepoName(repository.name());
        linked.setFullName(repository.fullName());
        linked.setHtmlUrl(repository.htmlUrl());
        linked.setVisibility(repository.visibility());
        linked.setLinkedBy(currentUser.id());

        ProjectGitHubRepository saved = projectGitHubRepositoryRepository.save(linked);
        return GitHubRepositoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public GitHubRepositoryResponse getLinkedRepository(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        ensureProjectAccess(project);

        ProjectGitHubRepository linked = projectGitHubRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project GitHub repository", projectId));

        return GitHubRepositoryResponse.from(linked);
    }

    @Transactional(readOnly = true)
    public ProjectGitHubRepository requireLinkedRepositoryWithProjectAccess(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        ensureProjectAccess(project);
        return projectGitHubRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project GitHub repository", projectId));
    }

    @Transactional
    public void unlinkRepository(Long projectId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!isProjectOwner(currentUser, project)) {
            throw new ForbiddenException("Only the project owner may unlink a GitHub repository");
        }

        ProjectGitHubRepository linked = projectGitHubRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project GitHub repository", projectId));

        if (contributionRepository.hasVerifiedContributions(linked.getId())) {
            throw new BadRequestException("A repository with verified contribution history cannot be unlinked");
        }

        projectGitHubRepositoryRepository.delete(linked);
    }

    private void ensureProjectAccess(Project project) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (isPrivileged(currentUser)) {
            return;
        }

        if (isProjectOwner(currentUser, project)) {
            return;
        }

        if (projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUser.id())
                .map(member -> "ACTIVE".equalsIgnoreCase(member.getStatus()))
                .orElse(false)) {
            return;
        }

        throw new ForbiddenException("You do not have access to that project repository");
    }

    private boolean isProjectOwner(CurrentUser currentUser, Project project) {
        if (project.getCreatedBy().equals(currentUser.id())) {
            return true;
        }

        return projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUser.id())
                .map(ProjectMember::getRole)
                .map(role -> "OWNER".equalsIgnoreCase(role))
                .orElse(false);
    }

    private boolean isPrivileged(CurrentUser currentUser) {
        return currentUser.roles().contains("SUPER_ADMIN") || currentUser.roles().contains("CORE_MEMBER");
    }
}

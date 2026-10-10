package com.mittechkernel.backend.modules.project.service;

import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubCommit;
import com.mittechkernel.backend.common.exception.UpstreamServiceException;
import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.project.dto.GitHubContributionResponse;
import com.mittechkernel.backend.modules.project.entity.ProjectGitHubRepository;
import com.mittechkernel.backend.modules.project.repository.GitHubContributionRepository;
import com.mittechkernel.backend.modules.user.service.GitHubConnectionService;
import com.mittechkernel.backend.modules.gamification.XpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GitHubContributionService {

    private final ProjectGitHubRepositoryService projectRepositoryService;
    private final GitHubConnectionService connectionService;
    private final GitHubClient gitHubClient;
    private final GitHubContributionRepository contributionRepository;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;
    private final XpService xpService;

    public GitHubContributionService(ProjectGitHubRepositoryService projectRepositoryService,
                                     GitHubConnectionService connectionService,
                                     GitHubClient gitHubClient,
                                     GitHubContributionRepository contributionRepository,
                                     ProjectService projectService,
                                     CurrentUserService currentUserService,
                                     XpService xpService) {
        this.projectRepositoryService = projectRepositoryService;
        this.connectionService = connectionService;
        this.gitHubClient = gitHubClient;
        this.contributionRepository = contributionRepository;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
        this.xpService = xpService;
    }

    @Transactional
    public List<GitHubContributionResponse> syncAndList(Long projectId) {
        ProjectGitHubRepository repository = projectRepositoryService
                .requireLinkedRepositoryWithProjectAccess(projectId);
        String accessToken = connectionService.requireAccessTokenForUser(repository.getLinkedBy());
        List<GitHubCommit> commits;
        try {
            commits = gitHubClient.listCommits(accessToken, repository.getOwnerLogin(), repository.getRepoName());
        } catch (RuntimeException ex) {
            throw new UpstreamServiceException("GitHub commit retrieval failed");
        }
        for (GitHubCommit commit : commits) {
            Long kernelUserId = contributionRepository.resolveKernelUser(commit.authorId());
            contributionRepository.upsert(projectId, repository.getId(), commit, kernelUserId);
        }
        return contributionRepository.findByProjectId(projectId);
    }

    @Transactional
    public GitHubContributionResponse verify(Long projectId, Long contributionId) {
        projectService.assertCanReviewProject(projectId);
        ProjectGitHubRepository linked = projectRepositoryService.requireLinkedRepositoryWithProjectAccess(projectId);
        GitHubContributionRepository.ContributionForVerification contribution = contributionRepository
                .findForVerification(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub contribution", contributionId));
        if (!projectId.equals(contribution.projectId()) || !linked.getId().equals(contribution.repositoryId())) {
            throw new BadRequestException("Contribution does not belong to this project's linked repository");
        }
        validateContributionAuthor(projectId, contribution);
        if (contribution.verified()) {
            throw new BadRequestException("Contribution has already been verified");
        }
        int updated = contributionRepository.markVerified(contributionId, currentUserService.getCurrentUser().id());
        if (updated != 1) {
            throw new BadRequestException("Contribution has already been verified");
        }
        xpService.awardVerifiedContribution(contributionId);
        return contributionRepository.findByProjectId(projectId).stream()
                .filter(item -> item.id().equals(contributionId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("GitHub contribution", contributionId));
    }

    public void validateContributionAuthor(Long projectId,
            GitHubContributionRepository.ContributionForVerification contribution) {
        if (contribution.userId() == null || contribution.githubAuthorId() == null
                || !contributionRepository.identityMatches(contribution.githubAuthorId(), contribution.userId())) {
            throw new BadRequestException("Contribution author is not linked to a Kernel user by GitHub ID");
        }
        projectService.assertUserProgramMatchesProject(projectId, contribution.userId());
        if (!contribution.sha().matches("[0-9a-f]{40}")) {
            throw new BadRequestException("Contribution has an invalid commit SHA");
        }
        if (!contributionRepository.isActiveMember(projectId, contribution.userId())) {
            throw new ForbiddenException("Contribution author must be an active project member");
        }
    }
}

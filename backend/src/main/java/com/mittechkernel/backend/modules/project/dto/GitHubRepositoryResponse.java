package com.mittechkernel.backend.modules.project.dto;

import com.mittechkernel.backend.common.github.GitHubRepository;
import com.mittechkernel.backend.modules.project.entity.ProjectGitHubRepository;

import java.time.Instant;

public record GitHubRepositoryResponse(
        Long id,
        Long projectId,
        Long githubRepoId,
        String ownerLogin,
        String repoName,
        String fullName,
        String htmlUrl,
        String visibility,
        Long linkedBy,
        Instant linkedAt
) {
    public static GitHubRepositoryResponse from(ProjectGitHubRepository entity) {
        return new GitHubRepositoryResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getGithubRepoId(),
                entity.getOwnerLogin(),
                entity.getRepoName(),
                entity.getFullName(),
                entity.getHtmlUrl(),
                entity.getVisibility(),
                entity.getLinkedBy(),
                entity.getLinkedAt()
        );
    }

    public static GitHubRepositoryResponse from(GitHubRepository repository, Long projectId, Long linkedBy, Instant linkedAt) {
        return new GitHubRepositoryResponse(
                null,
                projectId,
                repository.id(),
                repository.ownerLogin(),
                repository.name(),
                repository.fullName(),
                repository.htmlUrl(),
                repository.visibility(),
                linkedBy,
                linkedAt
        );
    }
}

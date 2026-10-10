package com.mittechkernel.backend.modules.project.repository;

import com.mittechkernel.backend.modules.project.entity.ProjectGitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectGitHubRepositoryRepository extends JpaRepository<ProjectGitHubRepository, Long> {

    Optional<ProjectGitHubRepository> findByProjectId(Long projectId);

    Optional<ProjectGitHubRepository> findByGithubRepoId(Long githubRepoId);
}

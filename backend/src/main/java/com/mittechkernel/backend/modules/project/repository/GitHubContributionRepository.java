package com.mittechkernel.backend.modules.project.repository;

import com.mittechkernel.backend.common.github.GitHubCommit;
import com.mittechkernel.backend.modules.project.dto.GitHubContributionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class GitHubContributionRepository {

    private final JdbcTemplate jdbcTemplate;

    public GitHubContributionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(Long projectId, Long repositoryRowId, GitHubCommit commit, Long kernelUserId) {
        jdbcTemplate.update("""
                INSERT INTO github_contributions (
                    project_id, project_github_repository_id, user_id, github_author_id,
                    github_author_login, commit_sha, commit_url, commit_message,
                    committed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (project_github_repository_id, commit_sha) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    github_author_id = EXCLUDED.github_author_id,
                    github_author_login = EXCLUDED.github_author_login,
                    commit_url = EXCLUDED.commit_url,
                    commit_message = EXCLUDED.commit_message,
                    committed_at = EXCLUDED.committed_at,
                    updated_at = now()
                WHERE github_contributions.verified = false
                """, projectId, repositoryRowId, kernelUserId, commit.authorId(),
                commit.authorLogin() == null ? "" : commit.authorLogin(), commit.sha(),
                commit.url(), commit.message(), Timestamp.from(commit.committedAt()));
    }

    public List<GitHubContributionResponse> findByProjectId(Long projectId) {
        return jdbcTemplate.query("""
                SELECT id, commit_sha, commit_message, commit_url, github_author_login,
                       committed_at, user_id, verified, verified_at
                FROM github_contributions
                WHERE project_id = ?
                ORDER BY committed_at DESC, id DESC
                """, (rs, rowNum) -> new GitHubContributionResponse(
                rs.getLong("id"), rs.getString("commit_sha"), rs.getString("commit_message"),
                rs.getString("commit_url"), rs.getString("github_author_login"),
                rs.getTimestamp("committed_at").toInstant(),
                rs.getObject("user_id", Long.class), rs.getBoolean("verified"),
                rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toInstant()), projectId);
    }

    public Optional<ContributionForVerification> findForVerification(Long contributionId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.project_id, c.project_github_repository_id, c.user_id,
                       c.github_author_id, c.commit_sha, c.verified
                FROM github_contributions c WHERE c.id = ?
                """, rs -> rs.next() ? Optional.of(new ContributionForVerification(
                rs.getLong("id"), rs.getLong("project_id"), rs.getLong("project_github_repository_id"),
                rs.getObject("user_id", Long.class), rs.getObject("github_author_id", Long.class),
                rs.getString("commit_sha"), rs.getBoolean("verified"))) : Optional.empty(), contributionId);
    }

    public Optional<ContributionForVerification> findByRepositoryAndSha(Long repositoryId, String sha) {
        return jdbcTemplate.query("""
                SELECT c.id, c.project_id, c.project_github_repository_id, c.user_id,
                       c.github_author_id, c.commit_sha, c.verified
                FROM github_contributions c
                WHERE c.project_github_repository_id = ? AND c.commit_sha = ?
                """, rs -> rs.next() ? Optional.of(new ContributionForVerification(
                rs.getLong("id"), rs.getLong("project_id"), rs.getLong("project_github_repository_id"),
                rs.getObject("user_id", Long.class), rs.getObject("github_author_id", Long.class),
                rs.getString("commit_sha"), rs.getBoolean("verified"))) : Optional.empty(), repositoryId, sha);
    }

    public boolean isActiveMember(Long projectId, Long userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM project_members WHERE project_id = ? AND user_id = ? AND status = 'ACTIVE')",
                Boolean.class, projectId, userId));
    }

    public boolean hasVerifiedContributions(Long repositoryId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM github_contributions WHERE project_github_repository_id = ? AND verified = true)",
                Boolean.class, repositoryId));
    }

    public boolean identityMatches(Long githubAuthorId, Long userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM github_identities WHERE github_user_id = ? AND user_id = ?)",
                Boolean.class, githubAuthorId, userId));
    }

    public int markVerified(Long contributionId, Long verifierId) {
        return jdbcTemplate.update("UPDATE github_contributions SET verified = true, verified_by = ?, verified_at = now(), updated_at = now() WHERE id = ? AND verified = false",
                verifierId, contributionId);
    }

    public record ContributionForVerification(Long id, Long projectId, Long repositoryId, Long userId,
                                              Long githubAuthorId, String sha, boolean verified) {}

    public Long resolveKernelUser(Long githubAuthorId) {
        if (githubAuthorId == null) {
            return null;
        }
        return jdbcTemplate.query("SELECT user_id FROM github_identities WHERE github_user_id = ?",
                rs -> rs.next() ? rs.getLong("user_id") : null, githubAuthorId);
    }
}

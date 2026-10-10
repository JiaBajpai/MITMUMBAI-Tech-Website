package com.mittechkernel.backend.modules.user.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.mittechkernel.backend.modules.user.dashboard.DashboardResponse.*;

@Repository
public class DashboardRepository {
    private static final int RECENT_PROJECT_LIMIT = 5;
    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProfileSummary findProfile(Long userId) {
        return jdbc.queryForObject("""
                SELECT u.id, u.name, u.program, p.bio, p.avatar_url, p.github_url
                FROM users u LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.id = ?
                """, (rs, rowNum) -> new ProfileSummary(rs.getLong("id"), rs.getString("name"),
                rs.getString("program"), rs.getString("bio"), rs.getString("avatar_url"),
                rs.getString("github_url")), userId);
    }

    public TaskSummary findTaskSummary(Long userId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status = 'OPEN') AS assigned_open,
                    (SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status = 'COMPLETED') AS assigned_completed,
                    (SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status = 'VERIFIED') AS assigned_verified,
                    (SELECT COUNT(*) FROM task_completions WHERE user_id = ?) AS submitted_completions,
                    (SELECT COUNT(*) FROM task_completions WHERE user_id = ? AND verified = TRUE) AS verified_completions,
                    (SELECT COUNT(*) FROM task_completions WHERE user_id = ? AND verified = FALSE) AS unverified_completions
                """, (rs, rowNum) -> new TaskSummary(rs.getLong("assigned_open"),
                rs.getLong("assigned_completed"), rs.getLong("assigned_verified"),
                rs.getLong("submitted_completions"), rs.getLong("verified_completions"),
                rs.getLong("unverified_completions")), userId, userId, userId, userId, userId, userId);
    }

    public ProjectSummary findProjectSummary(Long userId) {
        long[] counts = jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE status = 'REQUESTED') AS requested,
                       COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active,
                       COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected
                FROM project_members WHERE user_id = ?
                """, (rs, rowNum) -> new long[]{rs.getLong("requested"), rs.getLong("active"),
                rs.getLong("rejected")}, userId);

        List<ProjectMembershipSummary> recent = jdbc.query("""
                SELECT p.id AS project_id, p.name AS project_name, p.status AS project_status,
                       p.program, d.id AS domain_id, d.name AS domain_name,
                       pm.role AS membership_role, pm.status AS membership_status, pm.created_at AS joined_at
                FROM project_members pm
                JOIN projects p ON p.id = pm.project_id
                JOIN domains d ON d.id = p.domain_id
                WHERE pm.user_id = ?
                ORDER BY pm.created_at DESC, pm.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new ProjectMembershipSummary(rs.getLong("project_id"),
                rs.getString("project_name"), rs.getString("project_status"), rs.getString("program"),
                rs.getLong("domain_id"), rs.getString("domain_name"), rs.getString("membership_role"),
                rs.getString("membership_status"), rs.getTimestamp("joined_at").toInstant()), userId,
                RECENT_PROJECT_LIMIT);
        return new ProjectSummary(counts[0], counts[1], counts[2], recent);
    }

    public AttendanceSummary findAttendanceSummary(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'PRESENT') AS present,
                       COUNT(*) FILTER (WHERE status = 'ABSENT') AS absent,
                       COUNT(*) FILTER (WHERE status = 'LATE') AS late,
                       COUNT(*) FILTER (WHERE status = 'EXCUSED') AS excused
                FROM attendance WHERE user_id = ?
                """, (rs, rowNum) -> new AttendanceSummary(rs.getLong("total"), rs.getLong("present"),
                rs.getLong("absent"), rs.getLong("late"), rs.getLong("excused")), userId);
    }

    public GitHubContributionSummary findGitHubContributionSummary(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE verified = TRUE) AS verified,
                       COUNT(*) FILTER (WHERE verified = FALSE) AS unverified
                FROM github_contributions WHERE user_id = ?
                """, (rs, rowNum) -> new GitHubContributionSummary(rs.getLong("total"),
                rs.getLong("verified"), rs.getLong("unverified")), userId);
    }

}

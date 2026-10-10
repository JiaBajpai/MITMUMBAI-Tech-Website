package com.mittechkernel.backend.modules.gamification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class XpRepository {
    private final JdbcTemplate jdbcTemplate;

    public XpRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean awardVerifiedTaskCompletion(Long completionId) {
        return jdbcTemplate.update("""
                INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id)
                SELECT c.user_id, ?, 'TASK', 'TASK_COMPLETION', c.id
                FROM task_completions c
                JOIN tasks t ON t.id = c.task_id
                WHERE c.id = ? AND c.verified = true AND t.status = 'VERIFIED'
                ON CONFLICT (source, ref_type, ref_id)
                    WHERE ref_type IS NOT NULL AND ref_id IS NOT NULL
                DO NOTHING
                """, XpPolicy.VERIFIED_TASK, completionId) == 1;
    }

    public boolean awardVerifiedContribution(Long contributionId) {
        return jdbcTemplate.update("""
                INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id)
                SELECT c.user_id, ?, 'CONTRIBUTION', 'GITHUB_CONTRIBUTION', c.id
                FROM github_contributions c
                JOIN github_identities gi
                  ON gi.github_user_id = c.github_author_id AND gi.user_id = c.user_id
                WHERE c.id = ? AND c.verified = true
                  AND c.verified_by IS NOT NULL AND c.verified_at IS NOT NULL
                ON CONFLICT (source, ref_type, ref_id)
                    WHERE ref_type IS NOT NULL AND ref_id IS NOT NULL
                DO NOTHING
                """, XpPolicy.VERIFIED_CONTRIBUTION, contributionId) == 1;
    }

    public boolean awardPresentAttendance(Long attendanceId) {
        return jdbcTemplate.update("""
                INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id)
                SELECT a.user_id, ?, 'SESSION', 'ATTENDANCE', a.id
                FROM attendance a
                WHERE a.id = ? AND a.status = 'PRESENT'
                ON CONFLICT (source, ref_type, ref_id)
                    WHERE ref_type IS NOT NULL AND ref_id IS NOT NULL
                DO NOTHING
                """, XpPolicy.PRESENT_ATTENDANCE, attendanceId) == 1;
    }

    public XpSummaryResponse findSummary(Long userId) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT source, COALESCE(SUM(amount), 0)::integer AS points
                FROM xp_events WHERE user_id = ? GROUP BY source ORDER BY source
                """, rs -> {
            while (rs.next()) breakdown.put(rs.getString("source"), rs.getInt("points"));
            return null;
        }, userId);
        int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
        return new XpSummaryResponse(total, Map.copyOf(breakdown));
    }
}

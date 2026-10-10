package com.mittechkernel.backend.modules.gamification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LeaderboardRepository {
    private static final String ATTRIBUTED_EVENTS = """
            attributed_events AS (
                SELECT e.user_id, e.amount, s.program
                FROM xp_events e
                JOIN task_completions tc ON tc.id = e.ref_id
                     AND tc.user_id = e.user_id AND tc.verified = true
                JOIN tasks t ON t.id = tc.task_id AND t.status = 'VERIFIED'
                JOIN sessions s ON s.id = t.session_id
                WHERE e.source = 'TASK' AND e.ref_type = 'TASK_COMPLETION'
                UNION ALL
                SELECT e.user_id, e.amount, p.program
                FROM xp_events e
                JOIN github_contributions c ON c.id = e.ref_id
                     AND c.user_id = e.user_id AND c.verified = true
                     AND c.verified_by IS NOT NULL AND c.verified_at IS NOT NULL
                JOIN projects p ON p.id = c.project_id
                WHERE e.source = 'CONTRIBUTION' AND e.ref_type = 'GITHUB_CONTRIBUTION'
                UNION ALL
                SELECT e.user_id, e.amount, s.program
                FROM xp_events e
                JOIN attendance a ON a.id = e.ref_id
                     AND a.user_id = e.user_id AND a.status = 'PRESENT'
                JOIN sessions s ON s.id = a.session_id
                WHERE e.source = 'SESSION' AND e.ref_type = 'ATTENDANCE'
            )
            """;

    private static final String DOMAIN_EVENTS = """
            WITH scoped_events AS (
                SELECT e.user_id, e.amount
                FROM xp_events e
                JOIN task_completions tc ON tc.id = e.ref_id AND tc.user_id = e.user_id AND tc.verified = true
                JOIN tasks t ON t.id = tc.task_id AND t.status = 'VERIFIED'
                JOIN sessions s ON s.id = t.session_id
                WHERE e.source = 'TASK' AND e.ref_type = 'TASK_COMPLETION'
                  AND s.domain_id = ? AND s.program = 'TECHNICAL'
                UNION ALL
                SELECT e.user_id, e.amount
                FROM xp_events e
                JOIN github_contributions c ON c.id = e.ref_id
                     AND c.user_id = e.user_id AND c.verified = true
                     AND c.verified_by IS NOT NULL AND c.verified_at IS NOT NULL
                JOIN projects p ON p.id = c.project_id
                WHERE e.source = 'CONTRIBUTION' AND e.ref_type = 'GITHUB_CONTRIBUTION'
                  AND p.domain_id = ? AND p.program = 'TECHNICAL'
                UNION ALL
                SELECT e.user_id, e.amount
                FROM xp_events e
                JOIN attendance a ON a.id = e.ref_id AND a.user_id = e.user_id AND a.status = 'PRESENT'
                JOIN sessions s ON s.id = a.session_id
                WHERE e.source = 'SESSION' AND e.ref_type = 'ATTENDANCE'
                  AND s.domain_id = ? AND s.program = 'TECHNICAL'
            ), totals AS (
                SELECT user_id, SUM(amount)::bigint AS total_xp
                FROM scoped_events GROUP BY user_id
            ), participants AS (
                SELECT user_id FROM domain_members WHERE domain_id = ?
                UNION
                SELECT user_id FROM domain_leads
                WHERE domain_id = ? AND start_date <= CURRENT_DATE
                  AND (end_date IS NULL OR end_date >= CURRENT_DATE)
                UNION
                SELECT user_id FROM scoped_events
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LeaderboardPage findOverall(String program, int page, int size, long offset) {
        List<LeaderboardEntry> entries = jdbcTemplate.query("WITH " + ATTRIBUTED_EVENTS + """
                , totals AS (
                    SELECT user_id, SUM(amount)::bigint AS total_xp
                    FROM attributed_events WHERE program = ? GROUP BY user_id
                ), ranked AS (
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY COALESCE(t.total_xp, 0) DESC, lower(u.name), u.id
                           ) AS rank,
                           u.id AS user_id, u.name, u.program,
                           COALESCE(t.total_xp, 0)::bigint AS total_xp
                    FROM users u LEFT JOIN totals t ON t.user_id = u.id
                    WHERE u.active = true AND u.program = ?
                )
                SELECT rank, user_id, name, program, total_xp
                FROM ranked ORDER BY rank OFFSET ? LIMIT ?
                """, (rs, rowNum) -> new LeaderboardEntry(
                rs.getLong("rank"), rs.getLong("user_id"), rs.getString("name"),
                rs.getString("program"), rs.getLong("total_xp")), program, program, offset, size);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE active = true AND program = ?", Long.class, program);
        return page(entries, page, size, total);
    }

    public Long findRank(Long userId) {
        String sql = """
                WITH target_account AS (
                    SELECT id, name, program FROM users WHERE id = ? AND active = TRUE
                ),
                """ + ATTRIBUTED_EVENTS + """
                , totals AS (
                    SELECT user_id, SUM(amount)::bigint AS total_xp
                    FROM attributed_events
                    WHERE program = (SELECT program FROM target_account)
                    GROUP BY user_id
                ), target AS (
                    SELECT u.id, u.name, u.program, COALESCE(t.total_xp, 0)::bigint AS total_xp
                    FROM target_account u LEFT JOIN totals t ON t.user_id = u.id
                )
                SELECT CASE WHEN NOT EXISTS (SELECT 1 FROM target) THEN NULL ELSE (
                    SELECT COUNT(*) + 1
                    FROM target t
                    JOIN users ahead_user ON ahead_user.active = TRUE AND ahead_user.program = t.program
                    LEFT JOIN totals ahead_xp ON ahead_xp.user_id = ahead_user.id
                    WHERE COALESCE(ahead_xp.total_xp, 0) > t.total_xp
                       OR (COALESCE(ahead_xp.total_xp, 0) = t.total_xp
                           AND (lower(ahead_user.name) < lower(t.name)
                                OR (lower(ahead_user.name) = lower(t.name) AND ahead_user.id < t.id)))
                ) END AS rank
                FROM (VALUES (1)) AS singleton(dummy)
                """;
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }

    public LeaderboardPage findDomain(Long domainId, int page, int size, long offset) {
        List<Object> args = domainArguments(domainId);
        args.add(offset);
        args.add(size);
        List<LeaderboardEntry> entries = jdbcTemplate.query(DOMAIN_EVENTS + """
                , ranked AS (
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY COALESCE(t.total_xp, 0) DESC, lower(u.name), u.id
                           ) AS rank,
                           u.id AS user_id, u.name, u.program,
                           COALESCE(t.total_xp, 0)::bigint AS total_xp
                    FROM participants p
                    JOIN users u ON u.id = p.user_id AND u.active = true AND u.program = 'TECHNICAL'
                    LEFT JOIN totals t ON t.user_id = u.id
                )
                SELECT rank, user_id, name, program, total_xp
                FROM ranked ORDER BY rank OFFSET ? LIMIT ?
                """, (rs, rowNum) -> new LeaderboardEntry(
                rs.getLong("rank"), rs.getLong("user_id"), rs.getString("name"),
                rs.getString("program"), rs.getLong("total_xp")), args.toArray());

        List<Object> countArgs = domainArguments(domainId);
        Long total = jdbcTemplate.queryForObject(DOMAIN_EVENTS + """
                , eligible AS (
                    SELECT DISTINCT u.id
                    FROM participants p
                    JOIN users u ON u.id = p.user_id AND u.active = true AND u.program = 'TECHNICAL'
                )
                SELECT COUNT(*) FROM eligible
                """, Long.class, countArgs.toArray());
        return page(entries, page, size, total == null ? 0 : total);
    }

    private List<Object> domainArguments(Long domainId) {
        return new java.util.ArrayList<>(List.of(domainId, domainId, domainId, domainId, domainId));
    }

    private LeaderboardPage page(List<LeaderboardEntry> entries, int page, int size, long total) {
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new LeaderboardPage(entries, page, size, total, totalPages);
    }
}

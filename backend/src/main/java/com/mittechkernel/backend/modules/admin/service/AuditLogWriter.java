package com.mittechkernel.backend.modules.admin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class AuditLogWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditLogWriter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void record(Long actorId, String action, Long targetId, Map<String, ?> metadata) {
        try {
            String json = objectMapper.writeValueAsString(metadata);
            jdbc.update("INSERT INTO audit_logs (actor_id, action, resource, resource_id, result, meta) VALUES (?, ?, 'USER', ?, 'SUCCESS', CAST(? AS jsonb))",
                    actorId, action, targetId, json);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to write administrative audit record", ex);
        }
    }
}

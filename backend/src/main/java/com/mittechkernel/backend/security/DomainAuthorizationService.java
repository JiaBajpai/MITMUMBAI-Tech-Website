package com.mittechkernel.backend.security;

import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DomainAuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public DomainAuthorizationService(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    public boolean hasCurrentUserDomainAccess(Long domainId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return hasDomainAccess(currentUser.id(), domainId);
    }

    public boolean hasDomainAccess(Long userId, Long domainId) {
        if (userId == null || domainId == null) {
            return false;
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (" +
                        "SELECT 1 FROM domain_leads " +
                        "WHERE user_id = ? AND domain_id = ? " +
                        "AND start_date <= ? AND (end_date IS NULL OR end_date >= ?)" +
                        ")",
                Boolean.class,
                userId,
                domainId,
                LocalDate.now(),
                LocalDate.now()
        );

        return Boolean.TRUE.equals(exists);
    }
}

package com.mittechkernel.backend.modules.session.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.session.dto.SessionResponse;
import com.mittechkernel.backend.modules.session.entity.Session;
import com.mittechkernel.backend.modules.session.repository.SessionRepository;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private static final Set<String> VALID_PROGRAMS = Set.of("TECHNICAL", "FOUNDATION");
    private static final Set<String> VALID_TYPES = Set.of("WORKSHOP", "HACKATHON", "TALK", "SOCIAL", "FOUNDATION");

    private final SessionRepository sessionRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JdbcTemplate jdbcTemplate;

    public SessionService(SessionRepository sessionRepository,
                          CurrentUserService currentUserService,
                          DomainAuthorizationService domainAuthorizationService,
                          JdbcTemplate jdbcTemplate) {
        this.sessionRepository = sessionRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(Long domainId, String program, String type, LocalDate date) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        String normalizedProgram = normalizeFilter(program, "program", VALID_PROGRAMS);
        String normalizedType = normalizeFilter(type, "type", VALID_TYPES);

        List<Session> sessions = sessionRepository.findAllByOrderByDateDesc();

        if (currentUser.roles().contains("DOMAIN_LEAD")) {
            Set<Long> assignedDomains = getAssignedDomainIds(currentUser.id());
            if (assignedDomains.isEmpty()) {
                return List.of();
            }
            sessions = sessions.stream()
                    .filter(session -> assignedDomains.contains(session.getDomainId()))
                    .toList();
        }

        if (domainId != null) {
            if (currentUser.roles().contains("DOMAIN_LEAD") && !hasDomainAccess(currentUser.id(), domainId)) {
                throw new ForbiddenException("You do not have access to that session domain");
            }
            sessions = sessions.stream()
                    .filter(session -> session.getDomainId().equals(domainId))
                    .toList();
        }

        if (normalizedProgram != null) {
            sessions = sessions.stream()
                    .filter(session -> normalizedProgram.equalsIgnoreCase(session.getProgram()))
                    .toList();
        }

        if (normalizedType != null) {
            sessions = sessions.stream()
                    .filter(session -> normalizedType.equalsIgnoreCase(session.getType()))
                    .toList();
        }

        if (date != null) {
            sessions = sessions.stream()
                    .filter(session -> session.getDate().equals(date))
                    .toList();
        }

        return sessions.stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));

        if (currentUser.roles().contains("DOMAIN_LEAD") && !hasDomainAccess(currentUser.id(), session.getDomainId())) {
            throw new ForbiddenException("You do not have access to that session's domain");
        }

        return SessionResponse.from(session);
    }

    private boolean hasDomainAccess(Long userId, Long domainId) {
        return domainId != null && domainAuthorizationService.hasDomainAccess(userId, domainId);
    }

    private Set<Long> getAssignedDomainIds(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT domain_id FROM domain_leads WHERE user_id = ? AND start_date <= CURRENT_DATE AND (end_date IS NULL OR end_date >= CURRENT_DATE)",
                Long.class,
                userId
        ).stream().collect(Collectors.toSet());
    }

    private String normalizeFilter(String value, String fieldName, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BadRequestException("Invalid " + fieldName + ": " + value);
        }
        return normalized;
    }
}

package com.mittechkernel.backend.modules.resource.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.resource.dto.ResourceResponse;
import com.mittechkernel.backend.modules.resource.entity.Resource;
import com.mittechkernel.backend.modules.resource.repository.ResourceRepository;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private static final Set<String> VALID_TYPES = Set.of(
            "DOCUMENTATION", "COURSE", "TUTORIAL", "VIDEO", "ARTICLE",
            "PRACTICE_PLATFORM", "GITHUB_REPO"
    );

    private static final Set<String> VALID_DIFFICULTIES = Set.of(
            "BEGINNER", "INTERMEDIATE", "ADVANCED"
    );

    private final ResourceRepository resourceRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JdbcTemplate jdbcTemplate;

    public ResourceService(ResourceRepository resourceRepository,
                          CurrentUserService currentUserService,
                          DomainAuthorizationService domainAuthorizationService,
                          JdbcTemplate jdbcTemplate) {
        this.resourceRepository = resourceRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResources(Long domainId, String type, String difficulty, String topic) {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        String normalizedType = normalizeFilter(type, "resource type", VALID_TYPES);
        String normalizedDifficulty = normalizeFilter(difficulty, "difficulty", VALID_DIFFICULTIES);
        String normalizedTopic = normalizeTopic(topic);

        if (currentUser.roles().contains("DOMAIN_LEAD")) {
            Set<Long> assignedDomains = getAssignedDomainIds(currentUser.id());

            if (domainId != null && !assignedDomains.contains(domainId)) {
                throw new ForbiddenException("You do not have access to that domain's resources");
            }

            if (domainId == null) {
                if (assignedDomains.isEmpty()) {
                    return List.of();
                }
                return resourceRepository.findAllByDomainIdInOrderByCreatedAtDesc(assignedDomains)
                        .stream()
                        .map(ResourceResponse::from)
                        .toList();
            }

            return resourceRepository.findFiltered(domainId, normalizedType, normalizedDifficulty, normalizedTopic)
                    .stream()
                    .map(ResourceResponse::from)
                    .toList();
        }

        return resourceRepository.findFiltered(domainId, normalizedType, normalizedDifficulty, normalizedTopic)
                .stream()
                .map(ResourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));

        if (currentUser.roles().contains("DOMAIN_LEAD") && !domainAuthorizationService.hasDomainAccess(currentUser.id(), resource.getDomainId())) {
            throw new ForbiddenException("You do not have access to that domain's resources");
        }

        return ResourceResponse.from(resource);
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

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        return topic.trim().toLowerCase(Locale.ROOT);
    }
}

package com.mittechkernel.backend.modules.gamification;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.domain.service.DomainService;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class LeaderboardService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> PROGRAMS = Set.of("TECHNICAL", "FOUNDATION");

    private final LeaderboardRepository leaderboardRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final DomainService domainService;

    public LeaderboardService(LeaderboardRepository leaderboardRepository,
                              CurrentUserService currentUserService,
                              DomainAuthorizationService domainAuthorizationService,
                              DomainService domainService) {
        this.leaderboardRepository = leaderboardRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.domainService = domainService;
    }

    @Transactional(readOnly = true)
    public LeaderboardPage getOverall(String program, int page, int size) {
        validatePage(page, size);
        String normalizedProgram = normalizeProgram(program);
        return leaderboardRepository.findOverall(normalizedProgram, page, size, (long) page * size);
    }

    @Transactional(readOnly = true)
    public LeaderboardPage getDomain(Long domainId, int page, int size) {
        validatePage(page, size);
        domainService.getDomain(domainId);
        var user = currentUserService.getCurrentUser();
        if (user.roles().contains("DOMAIN_LEAD")
                && !domainAuthorizationService.hasDomainAccess(user.id(), domainId)) {
            throw new ForbiddenException("You do not have access to this domain leaderboard");
        }
        return leaderboardRepository.findDomain(domainId, page, size, (long) page * size);
    }

    @Transactional(readOnly = true)
    public Long getRank(Long userId) {
        return leaderboardRepository.findRank(userId);
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private String normalizeProgram(String program) {
        String normalized = program == null || program.isBlank()
                ? "TECHNICAL" : program.trim().toUpperCase(Locale.ROOT);
        if (!PROGRAMS.contains(normalized)) throw new BadRequestException("Invalid program: " + program);
        return normalized;
    }
}

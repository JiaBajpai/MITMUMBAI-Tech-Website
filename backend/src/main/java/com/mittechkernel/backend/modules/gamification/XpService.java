package com.mittechkernel.backend.modules.gamification;

import com.mittechkernel.backend.common.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XpService {
    private final XpRepository xpRepository;
    private final CurrentUserService currentUserService;

    public XpService(XpRepository xpRepository, CurrentUserService currentUserService) {
        this.xpRepository = xpRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public boolean awardVerifiedTaskCompletion(Long completionId) {
        return xpRepository.awardVerifiedTaskCompletion(completionId);
    }

    @Transactional
    public boolean awardVerifiedContribution(Long contributionId) {
        return xpRepository.awardVerifiedContribution(contributionId);
    }

    @Transactional
    public boolean awardPresentAttendance(Long attendanceId) {
        return xpRepository.awardPresentAttendance(attendanceId);
    }

    @Transactional(readOnly = true)
    public XpSummaryResponse getMySummary() {
        return xpRepository.findSummary(currentUserService.getCurrentUser().id());
    }
}

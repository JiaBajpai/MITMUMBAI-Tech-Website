package com.mittechkernel.backend.modules.user.dashboard;

import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.gamification.LeaderboardService;
import com.mittechkernel.backend.modules.gamification.XpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CurrentUserService currentUserService;
    private final DashboardRepository dashboardRepository;
    private final XpService xpService;
    private final LeaderboardService leaderboardService;

    public DashboardService(CurrentUserService currentUserService, DashboardRepository dashboardRepository,
                            XpService xpService, LeaderboardService leaderboardService) {
        this.currentUserService = currentUserService;
        this.dashboardRepository = dashboardRepository;
        this.xpService = xpService;
        this.leaderboardService = leaderboardService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getMyDashboard() {
        Long userId = currentUserService.getCurrentUser().id();
        return new DashboardResponse(
                dashboardRepository.findProfile(userId),
                xpService.getMySummary(),
                dashboardRepository.findTaskSummary(userId),
                dashboardRepository.findProjectSummary(userId),
                dashboardRepository.findAttendanceSummary(userId),
                dashboardRepository.findGitHubContributionSummary(userId),
                leaderboardService.getRank(userId)
        );
    }
}

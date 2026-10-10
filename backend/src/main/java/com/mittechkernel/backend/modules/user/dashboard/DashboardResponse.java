package com.mittechkernel.backend.modules.user.dashboard;

import com.mittechkernel.backend.modules.gamification.XpSummaryResponse;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        ProfileSummary profile,
        XpSummaryResponse xp,
        TaskSummary tasks,
        ProjectSummary projects,
        AttendanceSummary attendance,
        GitHubContributionSummary githubContributions,
        Long leaderboardRank
) {
    public record ProfileSummary(Long userId, String name, String program, String bio,
                                 String avatarUrl, String githubUrl) {}

    public record TaskSummary(long assignedOpen, long assignedCompleted, long assignedVerified,
                              long submittedCompletions, long verifiedCompletions,
                              long unverifiedCompletions) {}

    public record ProjectSummary(long requestedMemberships, long activeMemberships,
                                 long rejectedMemberships, List<ProjectMembershipSummary> recentMemberships) {}

    public record ProjectMembershipSummary(Long projectId, String projectName, String projectStatus,
                                           String program, Long domainId, String domainName,
                                           String membershipRole, String membershipStatus, Instant joinedAt) {}

    public record AttendanceSummary(long total, long present, long absent, long late, long excused) {}

    public record GitHubContributionSummary(long total, long verified, long unverified) {}
}

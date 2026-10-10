package com.mittechkernel.backend.modules.gamification;

import java.util.List;

public record LeaderboardPage(
        List<LeaderboardEntry> entries,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

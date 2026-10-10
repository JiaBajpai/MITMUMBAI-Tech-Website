package com.mittechkernel.backend.modules.gamification;

public record LeaderboardEntry(Long rank, Long userId, String name, String program, long totalXp) {}

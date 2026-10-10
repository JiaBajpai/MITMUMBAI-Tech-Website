package com.mittechkernel.backend.modules.gamification;

import java.util.Map;

public record XpSummaryResponse(int totalXp, Map<String, Integer> breakdown) {}

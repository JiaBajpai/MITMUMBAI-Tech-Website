package com.mittechkernel.backend.modules.resource.dto;

import com.mittechkernel.backend.modules.resource.entity.Resource;

import java.util.Arrays;

public record ResourceResponse(
        Long id,
        Long sessionId,
        Long domainId,
        String title,
        String url,
        String type,
        String difficulty,
        String[] topics,
        Integer quality
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getSessionId(),
                resource.getDomainId(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getType(),
                resource.getDifficulty(),
                resource.getTopics() == null ? new String[0] : Arrays.copyOf(resource.getTopics(), resource.getTopics().length),
                resource.getQuality()
        );
    }
}

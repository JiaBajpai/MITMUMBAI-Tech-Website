package com.mittechkernel.backend.modules.domain.dto;

import com.mittechkernel.backend.modules.domain.entity.Domain;

public record DomainResponse(
        Long id,
        String name,
        String description,
        Integer displayOrder
) {
    public static DomainResponse from(Domain domain) {
        return new DomainResponse(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getDisplayOrder()
        );
    }
}

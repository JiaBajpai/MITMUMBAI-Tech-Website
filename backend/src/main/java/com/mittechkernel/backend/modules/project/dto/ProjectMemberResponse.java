package com.mittechkernel.backend.modules.project.dto;

import com.mittechkernel.backend.modules.project.entity.ProjectMember;

import java.time.Instant;

public record ProjectMemberResponse(
        Long id,
        Long projectId,
        Long userId,
        String role,
        String status,
        Long reviewedBy,
        Instant createdAt
) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getProjectId(),
                member.getUserId(),
                member.getRole(),
                member.getStatus(),
                member.getReviewedBy(),
                member.getCreatedAt()
        );
    }
}

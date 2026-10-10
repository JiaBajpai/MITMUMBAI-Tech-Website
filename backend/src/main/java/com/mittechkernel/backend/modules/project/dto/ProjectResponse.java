package com.mittechkernel.backend.modules.project.dto;

import com.mittechkernel.backend.modules.project.entity.Project;
import com.mittechkernel.backend.modules.project.entity.ProjectMember;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        Long domainId,
        String status,
        String program,
        String problem,
        String solution,
        List<String> technologies,
        List<String> requiredSkills,
        Integer expectedMembers,
        String outcome,
        Long createdBy,
        Long approvedBy,
        String reviewComment,
        Instant createdAt,
        Instant updatedAt,
        List<ProjectMemberResponse> members
) {
    public static ProjectResponse from(Project project, List<ProjectMember> members) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDomainId(),
                project.getStatus(),
                project.getProgram(),
                project.getProblem(),
                project.getSolution(),
                project.getTechnologies() == null ? List.of() : Arrays.asList(project.getTechnologies()),
                project.getRequiredSkills() == null ? List.of() : Arrays.asList(project.getRequiredSkills()),
                project.getExpectedMembers(),
                project.getOutcome(),
                project.getCreatedBy(),
                project.getApprovedBy(),
                project.getReviewComment(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                members == null ? List.of() : members.stream().map(ProjectMemberResponse::from).toList()
        );
    }
}

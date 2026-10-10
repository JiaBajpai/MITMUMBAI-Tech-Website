package com.mittechkernel.backend.modules.task.dto;

import com.mittechkernel.backend.modules.task.entity.Task;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record TaskResponse(
        Long id,
        Long sessionId,
        Long projectId,
        String title,
        List<String> requirements,
        Instant deadline,
        boolean verificationRequired,
        Long assigneeId,
        String status
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getSessionId(),
                task.getProjectId(),
                task.getTitle(),
                task.getRequirements() == null ? List.of() : Arrays.stream(task.getRequirements()).toList(),
                task.getDeadline(),
                task.isVerificationRequired(),
                task.getAssigneeId(),
                task.getStatus()
        );
    }
}

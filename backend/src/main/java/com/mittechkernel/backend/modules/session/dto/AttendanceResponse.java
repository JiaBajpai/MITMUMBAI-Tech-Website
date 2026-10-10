package com.mittechkernel.backend.modules.session.dto;

import com.mittechkernel.backend.modules.session.entity.Attendance;

import java.time.Instant;

public record AttendanceResponse(
        Long id,
        Long sessionId,
        Long userId,
        String status,
        Long markedBy,
        Instant createdAt
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getSessionId(),
                attendance.getUserId(),
                attendance.getStatus(),
                attendance.getMarkedBy(),
                attendance.getCreatedAt()
        );
    }
}

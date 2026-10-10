package com.mittechkernel.backend.modules.session.dto;

import com.mittechkernel.backend.modules.session.entity.Session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public record SessionResponse(
        Long id,
        Long domainId,
        String program,
        String topic,
        String type,
        LocalDate date,
        LocalTime time,
        String description,
        List<String> objectives,
        String instructor,
        Long leadId
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getDomainId(),
                session.getProgram(),
                session.getTopic(),
                session.getType(),
                session.getDate(),
                session.getTime(),
                session.getDescription(),
                session.getObjectives() == null ? List.of() : Arrays.asList(session.getObjectives()),
                session.getInstructor(),
                session.getLeadId()
        );
    }
}

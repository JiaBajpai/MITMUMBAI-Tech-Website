package com.mittechkernel.backend.modules.session.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.session.dto.AttendanceRequest;
import com.mittechkernel.backend.modules.session.dto.AttendanceResponse;
import com.mittechkernel.backend.modules.session.entity.Attendance;
import com.mittechkernel.backend.modules.session.entity.Session;
import com.mittechkernel.backend.modules.session.repository.AttendanceRepository;
import com.mittechkernel.backend.modules.session.repository.SessionRepository;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import com.mittechkernel.backend.modules.gamification.XpService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AttendanceService {

    private static final Set<String> VALID_STATUSES = Set.of("PRESENT", "ABSENT", "LATE", "EXCUSED");

    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JdbcTemplate jdbcTemplate;
    private final XpService xpService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             SessionRepository sessionRepository,
                             CurrentUserService currentUserService,
                             DomainAuthorizationService domainAuthorizationService,
                             JdbcTemplate jdbcTemplate,
                             XpService xpService) {
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.xpService = xpService;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceForSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!canManageAttendance(currentUser, session)) {
            throw new ForbiddenException("You do not have access to this session's attendance");
        }

        return attendanceRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return attendanceRepository.findByUserIdOrderByCreatedAtDesc(currentUser.id())
                .stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional
    public AttendanceResponse markAttendance(Long sessionId, AttendanceRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (!canManageAttendance(currentUser, session)) {
            throw new ForbiddenException("You do not have authority to manage attendance for this session");
        }

        if (request == null || request.userId() == null) {
            throw new BadRequestException("userId is required");
        }
        if (!userExists(request.userId())) {
            throw new BadRequestException("User not found: " + request.userId());
        }
        if (!session.getProgram().equals(userProgram(request.userId()))) {
            throw new BadRequestException("Attendance participant must belong to the session program");
        }

        String normalizedStatus = normalizeStatus(request.status());
        if (attendanceRepository.existsBySessionIdAndUserId(sessionId, request.userId())) {
            throw new BadRequestException("Attendance already exists for this user in this session");
        }

        Attendance attendance = new Attendance();
        attendance.setSessionId(sessionId);
        attendance.setUserId(request.userId());
        attendance.setStatus(normalizedStatus);
        attendance.setMarkedBy(currentUser.id());

        Attendance saved = attendanceRepository.save(attendance);
        if ("PRESENT".equals(saved.getStatus())) {
            attendanceRepository.flush();
            xpService.awardPresentAttendance(saved.getId());
        }
        return AttendanceResponse.from(saved);
    }

    private boolean canManageAttendance(CurrentUser currentUser, Session session) {
        if (currentUser.roles().contains("SUPER_ADMIN") || currentUser.roles().contains("CORE_MEMBER")) {
            return true;
        }
        if (currentUser.roles().contains("FACULTY")) {
            return false;
        }
        if (currentUser.id().equals(session.getLeadId())) {
            return true;
        }
        if ("FOUNDATION".equals(session.getProgram())) {
            return false;
        }
        return domainAuthorizationService.hasDomainAccess(currentUser.id(), session.getDomainId());
    }

    private String userProgram(Long userId) {
        return jdbcTemplate.queryForObject("SELECT program FROM users WHERE id = ?", String.class, userId);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new BadRequestException("Invalid attendance status: " + status);
        }
        return normalized;
    }

    private boolean userExists(Long userId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE id = ?)",
                Boolean.class,
                userId
        );
        return Boolean.TRUE.equals(exists);
    }
}

package com.mittechkernel.backend.modules.session.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.session.dto.AttendanceRequest;
import com.mittechkernel.backend.modules.session.dto.AttendanceResponse;
import com.mittechkernel.backend.modules.session.dto.SessionResponse;
import com.mittechkernel.backend.modules.session.service.AttendanceService;
import com.mittechkernel.backend.modules.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final SessionService sessionService;
    private final AttendanceService attendanceService;

    public SessionController(SessionService sessionService, AttendanceService attendanceService) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(
            @RequestParam(name = "domainId", required = false) Long domainId,
            @RequestParam(name = "program", required = false) String program,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.getSessions(domainId, program, type, date),
                request
        ));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable Long id,
                                                                  HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSession(id), request));
    }

    @GetMapping("/sessions/{id}/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getSessionAttendance(@PathVariable Long id,
                                                                                   HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAttendanceForSession(id), request));
    }

    @PostMapping("/sessions/{id}/attendance")
    public ResponseEntity<ApiResponse<AttendanceResponse>> markAttendance(@PathVariable Long id,
                                                                          @Valid @RequestBody AttendanceRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        AttendanceResponse response = attendanceService.markAttendance(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, httpServletRequest, HttpStatus.CREATED.value()));
    }

    @GetMapping("/me/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getMyAttendance(), request));
    }
}

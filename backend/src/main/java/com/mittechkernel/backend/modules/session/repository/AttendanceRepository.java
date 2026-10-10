package com.mittechkernel.backend.modules.session.repository;

import com.mittechkernel.backend.modules.session.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
    List<Attendance> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}

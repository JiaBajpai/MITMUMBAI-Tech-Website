package com.mittechkernel.backend.modules.session.repository;

import com.mittechkernel.backend.modules.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findAllByOrderByDateDesc();
    List<Session> findByDomainIdOrderByDateDesc(Long domainId);
}

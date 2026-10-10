package com.mittechkernel.backend.modules.domain.repository;

import com.mittechkernel.backend.modules.domain.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    List<Domain> findAllByOrderByDisplayOrderAsc();
}

package com.mittechkernel.backend.modules.project.repository;

import com.mittechkernel.backend.modules.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByCreatedAtDesc();
}

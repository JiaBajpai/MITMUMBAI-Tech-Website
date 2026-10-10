package com.mittechkernel.backend.modules.resource.repository;

import com.mittechkernel.backend.modules.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query(value = """
            SELECT *
            FROM resources
            WHERE (:domainId IS NULL OR domain_id = :domainId)
              AND (:type IS NULL OR type = :type)
              AND (:difficulty IS NULL OR difficulty = :difficulty)
              AND (:topic IS NULL OR :topic = ANY(topics))
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Resource> findFiltered(
            @Param("domainId") Long domainId,
            @Param("type") String type,
            @Param("difficulty") String difficulty,
            @Param("topic") String topic
    );

    @Query(value = """
            SELECT *
            FROM resources
            WHERE domain_id IN (:domainIds)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Resource> findAllByDomainIdInOrderByCreatedAtDesc(@Param("domainIds") Collection<Long> domainIds);
}

package com.example.mofawid.repositories;

import com.example.mofawid.models.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MissionRepo extends JpaRepository<Mission, UUID> {

    Page<Mission> findAllByCaseEntityTenantId(
            UUID tenantId,
            Pageable pageable
    );

    Optional<Mission> findByIdAndCaseEntityTenantId(
            UUID id,
            UUID tenantId
    );

    Page<Mission> findAllByAssignedToId(
            UUID userId,
            Pageable pageable
    );
}
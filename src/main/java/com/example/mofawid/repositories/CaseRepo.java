package com.example.mofawid.repositories;

import com.example.mofawid.models.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CaseRepo extends JpaRepository<Case, UUID> {

    Page<Case> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<Case> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Case> findByRefAndTenantId(String ref, UUID tenantId);
}
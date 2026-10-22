package com.example.mofawid.repositories;

import com.example.mofawid.models.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CaseRepo extends JpaRepository<Case, UUID> {

    Page<Case> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<Case> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Case> findByRefAndTenantId(String ref, UUID tenantId);
    boolean existsByTenantId(UUID tenantId);

    // the biggest register number of the office for one year (null if no case yet)
    @Query("select max(c.registerNumber) from Case c where c.tenant.id = :tenantId and c.registerYear = :year")
    Integer findMaxRegisterNumber(
            @Param("tenantId") UUID tenantId,
            @Param("year") Integer year
    );
}
package com.example.mofawid.repositories;

import com.example.mofawid.Enums.CaseStatus;
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

    // search in ALL the cases of the office (not only the current page)
    // status and search are optional: null = no filter
    @Query("select c from Case c where c.tenant.id = :tenantId" +
            " and (:status is null or c.caseStatus = :status)" +
            " and (:search is null" +
            "   or lower(c.ref) like :search" +
            "   or lower(c.clientName) like :search" +
            "   or lower(c.clientIce) like :search" +
            "   or lower(c.subject) like :search" +
            "   or lower(c.address) like :search)")
    Page<Case> searchCases(
            @Param("tenantId") UUID tenantId,
            @Param("status") CaseStatus status,
            @Param("search") String search,
            Pageable pageable
    );

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
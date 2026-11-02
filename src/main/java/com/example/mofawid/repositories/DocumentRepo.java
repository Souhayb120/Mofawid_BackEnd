package com.example.mofawid.repositories;

import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.models.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepo extends JpaRepository<Document, UUID> {

    Page<Document> findAllByCaseEntityId(
            UUID caseId,
            Pageable pageable
    );

    // all the documents of one case (used when the case is deleted)
    List<Document> findAllByCaseEntityId(UUID caseId);

    // true if the case has an official (approved) document
    boolean existsByCaseEntityIdAndStatus(UUID caseId, DocumentStatus status);

    Optional<Document> findByIdAndCaseEntityTenantId(
            UUID id,
            UUID tenantId
    );

    // all the documents of the office (the "Documents" page)
    // kind, status, search, fromDate and toDate are optional: null = no filter
    @Query("select d from Document d join d.caseEntity c where c.tenant.id = :tenantId" +
            " and (:kind is null or d.kind = :kind)" +
            " and (:status is null or d.status = :status)" +
            " and (:fromDate is null or d.createdAt >= :fromDate)" +
            " and (:toDate is null or d.createdAt < :toDate)" +
            " and (:search is null" +
            "   or lower(c.ref) like :search" +
            "   or lower(c.clientName) like :search" +
            "   or lower(c.subject) like :search)")
    Page<Document> searchDocuments(
            @Param("tenantId") UUID tenantId,
            @Param("kind") DocumentKind kind,
            @Param("status") DocumentStatus status,
            @Param("search") String search,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}
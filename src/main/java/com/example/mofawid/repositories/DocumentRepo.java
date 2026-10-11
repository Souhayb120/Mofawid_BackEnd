package com.example.mofawid.repositories;

import com.example.mofawid.models.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepo extends JpaRepository<Document, UUID> {

    Page<Document> findAllByCaseEntityId(
            UUID caseId,
            Pageable pageable
    );

    Optional<Document> findByIdAndCaseEntityTenantId(
            UUID id,
            UUID tenantId
    );
}
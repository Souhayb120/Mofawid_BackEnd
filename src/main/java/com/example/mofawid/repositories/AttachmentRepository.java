package com.example.mofawid.repositories;

import com.example.mofawid.models.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository
        extends JpaRepository<Attachment, UUID> {

    Page<Attachment> findAllByCaseEntityIdAndTenantId(
            UUID caseId,
            UUID tenantId,
            Pageable pageable
    );

    Optional<Attachment> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );
}
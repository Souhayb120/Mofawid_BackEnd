package com.example.mofawid.repositories;

import com.example.mofawid.models.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository
        extends JpaRepository<Attachment, UUID> {

    Page<Attachment> findAllByCaseEntityIdAndTenantId(
            UUID caseId,
            UUID tenantId,
            Pageable pageable
    );

    Page<Attachment> findAllByMissionIdAndTenantId(
            UUID missionId,
            UUID tenantId,
            Pageable pageable
    );

    // the files of a case in the order they were uploaded (photo 1, photo 2...)
    List<Attachment> findAllByCaseEntityIdOrderByUploadedAtAsc(UUID caseId);

    Optional<Attachment> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );
}
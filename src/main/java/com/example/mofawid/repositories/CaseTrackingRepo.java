package com.example.mofawid.repositories;

import com.example.mofawid.models.CaseTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CaseTrackingRepo
        extends JpaRepository<CaseTracking, UUID> {

    Optional<CaseTracking> findByCaseEntityId(
            UUID caseId
    );

    Optional<CaseTracking> findByCodeHash(
            String codeHash
    );
}
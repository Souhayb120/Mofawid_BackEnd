package com.example.mofawid.repositories;

import com.example.mofawid.models.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingRepo extends JpaRepository<Billing, UUID> {

    Optional<Billing> findByCaseEntityId(UUID caseId);

    Optional<Billing> findByIdAndCaseEntityTenantId(
            UUID id,
            UUID tenantId
    );
}
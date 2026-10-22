package com.example.mofawid.repositories;

import com.example.mofawid.models.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyRepo extends JpaRepository<Party, UUID> {

    List<Party> findAllByCaseEntityId(UUID caseId);

    Optional<Party> findByIdAndCaseEntityTenantId(
            UUID id,
            UUID tenantId
    );
}

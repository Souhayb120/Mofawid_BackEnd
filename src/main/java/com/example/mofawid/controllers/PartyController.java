package com.example.mofawid.controllers;

import com.example.mofawid.DTO.PartyDTO.PartyRequestDTO;
import com.example.mofawid.DTO.PartyDTO.PartyResponseDTO;
import com.example.mofawid.services.PartyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @PostMapping("/api/cases/{caseId}/parties")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<PartyResponseDTO> addParty(
            @PathVariable UUID caseId,
            @Valid @RequestBody PartyRequestDTO dto
    ) {
        return ResponseEntity.ok(
                partyService.addParty(caseId, dto)
        );
    }

    @GetMapping("/api/cases/{caseId}/parties")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<List<PartyResponseDTO>> getCaseParties(
            @PathVariable UUID caseId
    ) {
        return ResponseEntity.ok(
                partyService.getCaseParties(caseId)
        );
    }

    @PutMapping("/api/parties/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<PartyResponseDTO> updateParty(
            @PathVariable UUID id,
            @Valid @RequestBody PartyRequestDTO dto
    ) {
        return ResponseEntity.ok(
                partyService.updateParty(id, dto)
        );
    }

    @DeleteMapping("/api/parties/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<Void> deleteParty(
            @PathVariable UUID id
    ) {
        partyService.deleteParty(id);

        return ResponseEntity.noContent().build();
    }
}

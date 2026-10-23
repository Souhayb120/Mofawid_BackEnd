package com.example.mofawid.controllers;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.services.CaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<CaseResponseDTO> createCase(
            @Valid @RequestBody CaseRequestDTO dto
    ) {
        return ResponseEntity.ok(
                caseService.createCase(dto)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<Page<CaseResponseDTO>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            // optional filters: ?search=2026/12&status=IN_PROGRESS
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CaseStatus status
    ) {
        return ResponseEntity.ok(
                caseService.getAllCases(page, size, search, status)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<CaseResponseDTO> getCaseById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                caseService.getCaseById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<CaseResponseDTO> updateCase(
            @PathVariable UUID id,
            @Valid @RequestBody CaseRequestDTO dto
    ) {
        return ResponseEntity.ok(
                caseService.updateCase(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCase(
            @PathVariable UUID id
    ) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }
}
package com.example.mofawid.controllers;

import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.services.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService
    ) {
        this.documentService = documentService;
    }

    @PostMapping("/api/cases/{caseId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<DocumentResponseDTO> createDocument(
            @PathVariable UUID caseId,
            @Valid @RequestBody DocumentRequestDTO dto
    ) {
        return ResponseEntity.ok(
                documentService.createDocument(caseId, dto)
        );
    }

    @GetMapping("/api/cases/{caseId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<Page<DocumentResponseDTO>> getCaseDocuments(
            @PathVariable UUID caseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.getCaseDocuments(
                        caseId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/api/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                documentService.getDocumentById(id)
        );
    }

    @PutMapping("/api/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<DocumentResponseDTO> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequestDTO dto
    ) {
        return ResponseEntity.ok(
                documentService.updateDocument(id, dto)
        );
    }

    @DeleteMapping("/api/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id
    ) {
        documentService.deleteDocument(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/cases/{caseId}/ai-draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<DocumentResponseDTO> generateAiDraft(
            @PathVariable UUID caseId,
            @RequestParam DocumentKind kind
    ) {
        return ResponseEntity.ok(
                documentService.generateAiDraft(caseId, kind)
        );
    }



    @PatchMapping("/api/documents/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<DocumentResponseDTO> submitForApproval(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                documentService.submitForApproval(id)
        );
    }

    @PatchMapping("/api/documents/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponseDTO> approveDocument(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                documentService.approveDocument(id)
        );
    }
}
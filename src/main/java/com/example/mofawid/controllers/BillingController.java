package com.example.mofawid.controllers;


import com.example.mofawid.DTO.BillingDTO.BillingRequestDTO;
import com.example.mofawid.DTO.BillingDTO.BillingResponseDTO;
import com.example.mofawid.services.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(
            BillingService billingService
    ) {
        this.billingService = billingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<BillingResponseDTO> createBilling(
            @PathVariable UUID caseId,
            @Valid @RequestBody BillingRequestDTO dto
    ) {

        return ResponseEntity.ok(
                billingService.createBilling(
                        caseId,
                        dto
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<BillingResponseDTO> getBilling(
            @PathVariable UUID caseId
    ) {

        return ResponseEntity.ok(
                billingService.getBillingByCase(
                        caseId
                )
        );
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<BillingResponseDTO> updateBilling(
            @PathVariable UUID caseId,
            @Valid @RequestBody BillingRequestDTO dto
    ) {

        return ResponseEntity.ok(
                billingService.updateBilling(
                        caseId,
                        dto
                )
        );
    }
}
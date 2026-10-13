package com.example.mofawid.controllers;

import com.example.mofawid.DTO.TrackingDTO.ClientTrackingResponseDTO;
import com.example.mofawid.DTO.TrackingDTO.TrackingCodeResponseDTO;
import com.example.mofawid.services.CaseTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class CaseTrackingController {

    private final CaseTrackingService
            caseTrackingService;

    public CaseTrackingController(
            CaseTrackingService caseTrackingService
    ) {
        this.caseTrackingService =
                caseTrackingService;
    }

    @PostMapping(
            "/api/cases/{caseId}/tracking-code"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ASSISTANT')"
    )
    public ResponseEntity<TrackingCodeResponseDTO>
    generateTrackingCode(
            @PathVariable UUID caseId
    ) {

        return ResponseEntity.ok(
                caseTrackingService
                        .generateTrackingCode(
                                caseId
                        )
        );
    }

    @GetMapping(
            "/api/public/tracking/{code}"
    )
    public ResponseEntity<ClientTrackingResponseDTO>
    trackCase(
            @PathVariable String code
    ) {

        return ResponseEntity.ok(
                caseTrackingService
                        .trackCase(code)
        );
    }
}
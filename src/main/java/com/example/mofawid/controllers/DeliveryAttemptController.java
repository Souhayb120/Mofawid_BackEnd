package com.example.mofawid.controllers;

import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptRequestDTO;
import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptResponseDTO;
import com.example.mofawid.services.DeliveryAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions/{missionId}/attempts")
public class DeliveryAttemptController {

    private final DeliveryAttemptService deliveryAttemptService;

    public DeliveryAttemptController(DeliveryAttemptService deliveryAttemptService) {
        this.deliveryAttemptService = deliveryAttemptService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<DeliveryAttemptResponseDTO> addAttempt(
            @PathVariable UUID missionId,
            @Valid @RequestBody DeliveryAttemptRequestDTO dto
    ) {
        return ResponseEntity.ok(
                deliveryAttemptService.addAttempt(missionId, dto)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT', 'COLLABORATEUR')")
    public ResponseEntity<List<DeliveryAttemptResponseDTO>> getMissionAttempts(
            @PathVariable UUID missionId
    ) {
        return ResponseEntity.ok(
                deliveryAttemptService.getMissionAttempts(missionId)
        );
    }
}

package com.example.mofawid.controllers;

import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import com.example.mofawid.services.MissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    // ADMIN creates a mission and assigns it to a collaborateur
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MissionResponseDTO> createMission(
            @Valid @RequestBody MissionRequestDTO dto
    ) {
        return ResponseEntity.ok(
                missionService.createMission(dto)
        );
    }

    // ADMIN sees all missions of his tenant
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<MissionResponseDTO>> getAllMissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                missionService.getAllMissions(page, size)
        );
    }

    // ADMIN or COLLABORATEUR can open one mission
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> getMissionById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                missionService.getMissionById(id)
        );
    }

    // Collaborateur sees only his missions
    @GetMapping("/my")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<Page<MissionResponseDTO>> getMyMissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                missionService.getMyMissions(page, size)
        );
    }

    // Collaborateur starts his mission
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> startMission(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                missionService.startMission(id)
        );
    }

    // Collaborateur completes his mission
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> completeMission(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                missionService.completeMission(id)
        );
    }
}
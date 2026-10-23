package com.example.mofawid.controllers;

import com.example.mofawid.DTO.MissionDTO.MissionPresenceDTO;
import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import com.example.mofawid.services.MissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MissionResponseDTO> createMission(
            @Valid @RequestBody MissionRequestDTO dto
    ) {
        return ResponseEntity.ok(
                missionService.createMission(dto)
        );
    }

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

    // the missions of one case (tab "Missions" of the case page)
    @GetMapping("/by-case/{caseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MissionResponseDTO>> getMissionsByCase(
            @PathVariable UUID caseId
    ) {
        return ResponseEntity.ok(
                missionService.getMissionsByCase(caseId)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> getMissionById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                missionService.getMissionById(id)
        );
    }

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

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> startMission(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                missionService.startMission(id)
        );
    }

    // constat: the person present (بحضور وتحت عهدة ومسؤولية ...)
    @PatchMapping("/{id}/presence")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<MissionResponseDTO> updatePresence(
            @PathVariable UUID id,
            @Valid @RequestBody MissionPresenceDTO dto
    ) {
        return ResponseEntity.ok(
                missionService.updatePresence(id, dto)
        );
    }

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
package com.example.mofawid.controllers;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import com.example.mofawid.DTO.ObservationDTO.TranscriptionResponseDTO;
import com.example.mofawid.services.ObservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/missions/{missionId}/observations")
public class ObservationController {

    private final ObservationService observationService;

    public ObservationController(
            ObservationService observationService
    ) {
        this.observationService = observationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<ObservationResponseDTO> createObservation(
            @PathVariable UUID missionId,
            @Valid @RequestBody ObservationRequestDTO dto
    ) {

        return ResponseEntity.ok(
                observationService.createObservation(
                        missionId,
                        dto
                )
        );
    }

    // voice recording -> text (the collaborateur checks the text, then saves the observation)
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<TranscriptionResponseDTO> transcribeAudio(
            @PathVariable UUID missionId,
            @RequestParam("audio") MultipartFile audio
    ) {

        return ResponseEntity.ok(
                observationService.transcribeAudio(
                        missionId,
                        audio
                )
        );
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ASSISTANT', 'COLLABORATEUR')"
    )
    public ResponseEntity<Page<ObservationResponseDTO>>
    getMissionObservations(
            @PathVariable UUID missionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                observationService.getMissionObservations(
                        missionId,
                        page,
                        size
                )
        );
    }
}
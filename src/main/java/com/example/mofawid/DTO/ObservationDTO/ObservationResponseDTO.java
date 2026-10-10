package com.example.mofawid.DTO.ObservationDTO;

import com.example.mofawid.Enums.ObservationKind;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ObservationResponseDTO {

    private UUID id;

    private UUID missionId;

    private ObservationKind kind;

    private String description;

    private Instant observedAt;
}
package com.example.mofawid.DTO.MissionDTO;

import com.example.mofawid.Enums.MissionStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MissionResponseDTO {

    private UUID id;

    private UUID caseId;
    private String caseRef;

    private UUID assignedToId;
    private String assignedToUsername;

    private UUID assignedById;
    private String assignedByUsername;

    private String title;
    private String description;

    private LocalDateTime scheduledAt;

    private String presentPersonName;

    private String presentPersonCapacity;

    private MissionStatus status;

    private Instant createdAt;
}
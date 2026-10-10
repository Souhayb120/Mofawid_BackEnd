package com.example.mofawid.DTO.MissionDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MissionRequestDTO {

    @NotNull
    private UUID caseId;

    @NotNull
    private UUID collaborateurId;

    @NotBlank
    private String title;

    private String description;

    private LocalDateTime scheduledAt;
}
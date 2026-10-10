package com.example.mofawid.DTO.CaseDTO;

import com.example.mofawid.Enums.CaseStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CaseResponseDTO {

    private UUID id;

    private String ref;

    private CaseStatus caseStatus;

    private String clientName;

    private String clientIce;

    private String address;

    private String phone;

    private String subject;

    private String description;

    private LocalDate deadline;

    private UUID createdById;

    private String createdByUsername;

    private Instant createdAt;
}
package com.example.mofawid.DTO.DocumentDTO;

import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class DocumentResponseDTO {

    private UUID id;

    private UUID caseId;

    private String caseRef;

    private DocumentKind kind;

    private DocumentStatus status;

    private String content;

    private Instant createdAt;

    private Instant approvedAt;

    private String approvedByUsername;
}
package com.example.mofawid.DTO.AttachmentDTO;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO {

    private UUID id;
    private String originalName;
    private String contentType;
    private Long size;

    private String caption;
    private Instant uploadedAt;
    private UUID caseId;
    private UUID uploadedById;
}
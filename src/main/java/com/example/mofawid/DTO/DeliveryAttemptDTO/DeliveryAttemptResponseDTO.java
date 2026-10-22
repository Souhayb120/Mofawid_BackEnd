package com.example.mofawid.DTO.DeliveryAttemptDTO;

import com.example.mofawid.Enums.DeliveryResult;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DeliveryAttemptResponseDTO {

    private UUID id;

    private UUID missionId;

    private LocalDateTime attemptedAt;

    private DeliveryResult result;

    private String receiverName;

    private String receiverCin;

    private String receiverCapacity;

    private Boolean signed;

    private Boolean visitCardLeft;

    private String notes;

    private Instant createdAt;
}

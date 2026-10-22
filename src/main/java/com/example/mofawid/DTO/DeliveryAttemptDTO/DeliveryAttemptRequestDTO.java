package com.example.mofawid.DTO.DeliveryAttemptDTO;

import com.example.mofawid.Enums.DeliveryResult;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryAttemptRequestDTO {

    // if empty, we use now
    private LocalDateTime attemptedAt;

    @NotNull
    private DeliveryResult result;

    private String receiverName;

    private String receiverCin;

    private String receiverCapacity;

    private Boolean signed;

    private Boolean visitCardLeft;

    private String notes;
}

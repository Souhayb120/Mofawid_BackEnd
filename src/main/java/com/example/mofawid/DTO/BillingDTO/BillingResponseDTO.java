package com.example.mofawid.DTO.BillingDTO;

import com.example.mofawid.Enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class BillingResponseDTO {

    private UUID id;

    private UUID caseId;

    private String caseRef;

    private BigDecimal totalAmount;

    private BigDecimal amountPaid;

    private BigDecimal remainingAmount;

    private PaymentStatus status;

    private String description;

    private Instant createdAt;
}
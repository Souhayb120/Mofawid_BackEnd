package com.example.mofawid.DTO.TrackingDTO;

import com.example.mofawid.Enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClientTrackingResponseDTO {

    private String caseRef;

    private String progress;

    private boolean reportReady;

    private BigDecimal totalAmount;

    private BigDecimal amountPaid;

    private BigDecimal remainingAmount;

    private PaymentStatus paymentStatus;
}
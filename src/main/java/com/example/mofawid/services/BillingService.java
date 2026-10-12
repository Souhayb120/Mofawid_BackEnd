package com.example.mofawid.services;

import com.example.mofawid.DTO.BillingDTO.BillingRequestDTO;
import com.example.mofawid.DTO.BillingDTO.BillingResponseDTO;

import java.util.UUID;

public interface BillingService {

    BillingResponseDTO createBilling(
            UUID caseId,
            BillingRequestDTO dto
    );

    BillingResponseDTO getBillingByCase(
            UUID caseId
    );

    BillingResponseDTO updateBilling(
            UUID caseId,
            BillingRequestDTO dto
    );
}
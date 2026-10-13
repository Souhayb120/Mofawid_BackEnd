package com.example.mofawid.services;

import com.example.mofawid.DTO.TrackingDTO.ClientTrackingResponseDTO;
import com.example.mofawid.DTO.TrackingDTO.TrackingCodeResponseDTO;

import java.util.UUID;

public interface CaseTrackingService {

    TrackingCodeResponseDTO generateTrackingCode(
            UUID caseId
    );

    ClientTrackingResponseDTO trackCase(
            String trackingCode
    );
}
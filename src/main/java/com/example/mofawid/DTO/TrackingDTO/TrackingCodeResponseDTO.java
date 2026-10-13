package com.example.mofawid.DTO.TrackingDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrackingCodeResponseDTO {

    private String caseRef;

    private String trackingCode;
}
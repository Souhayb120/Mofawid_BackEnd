package com.example.mofawid.DTO.PartyDTO;

import com.example.mofawid.Enums.PartyRole;
import com.example.mofawid.Enums.PartyType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PartyResponseDTO {

    private UUID id;

    private UUID caseId;

    private PartyRole role;

    private PartyType type;

    private String fullName;

    private String cin;

    private String rc;

    private String legalRepresentative;

    private String address;

    private String phone;

    private String capacity;

    private Instant createdAt;
}

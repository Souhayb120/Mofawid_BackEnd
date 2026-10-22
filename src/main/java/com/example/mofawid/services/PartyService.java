package com.example.mofawid.services;

import com.example.mofawid.DTO.PartyDTO.PartyRequestDTO;
import com.example.mofawid.DTO.PartyDTO.PartyResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PartyService {

    PartyResponseDTO addParty(UUID caseId, PartyRequestDTO dto);

    List<PartyResponseDTO> getCaseParties(UUID caseId);

    PartyResponseDTO updateParty(UUID id, PartyRequestDTO dto);

    void deleteParty(UUID id);
}

package com.example.mofawid.services;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CaseService {

    CaseResponseDTO createCase(CaseRequestDTO dto);

    CaseResponseDTO getCaseById(UUID id);

    Page<CaseResponseDTO> getAllCases(int page, int size, String search, CaseStatus status);

    CaseResponseDTO updateCase(UUID id, CaseRequestDTO dto);

    void deleteCase(UUID id);
}
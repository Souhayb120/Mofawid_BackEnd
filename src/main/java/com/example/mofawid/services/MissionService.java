package com.example.mofawid.services;

import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MissionService {

    MissionResponseDTO createMission(MissionRequestDTO dto);

    MissionResponseDTO getMissionById(UUID id);

    Page<MissionResponseDTO> getAllMissions(
            int page,
            int size
    );

    Page<MissionResponseDTO> getMyMissions(
            int page,
            int size
    );

    MissionResponseDTO startMission(UUID id);

    MissionResponseDTO completeMission(UUID id);
}
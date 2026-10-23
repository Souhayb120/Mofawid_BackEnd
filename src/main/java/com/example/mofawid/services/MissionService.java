package com.example.mofawid.services;

import com.example.mofawid.DTO.MissionDTO.MissionPresenceDTO;
import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface MissionService {

    MissionResponseDTO createMission(MissionRequestDTO dto);

    MissionResponseDTO getMissionById(UUID id);

    Page<MissionResponseDTO> getAllMissions(
            int page,
            int size
    );

    List<MissionResponseDTO> getMissionsByCase(UUID caseId);

    Page<MissionResponseDTO> getMyMissions(
            int page,
            int size
    );

    MissionResponseDTO startMission(UUID id);

    MissionResponseDTO completeMission(UUID id);

    MissionResponseDTO updatePresence(UUID id, MissionPresenceDTO dto);
}
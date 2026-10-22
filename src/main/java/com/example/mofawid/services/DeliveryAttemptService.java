package com.example.mofawid.services;

import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptRequestDTO;
import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptResponseDTO;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptService {

    DeliveryAttemptResponseDTO addAttempt(UUID missionId, DeliveryAttemptRequestDTO dto);

    List<DeliveryAttemptResponseDTO> getMissionAttempts(UUID missionId);
}

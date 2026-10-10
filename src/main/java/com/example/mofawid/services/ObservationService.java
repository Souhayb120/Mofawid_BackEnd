package com.example.mofawid.services;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ObservationService {

    ObservationResponseDTO createObservation(
            UUID missionId,
            ObservationRequestDTO dto
    );

    Page<ObservationResponseDTO> getMissionObservations(
            UUID missionId,
            int page,
            int size
    );
}
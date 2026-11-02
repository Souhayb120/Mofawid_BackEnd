package com.example.mofawid.services;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import com.example.mofawid.DTO.ObservationDTO.TranscriptionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

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

    // voice recording -> text, the observation is NOT saved here
    TranscriptionResponseDTO transcribeAudio(
            UUID missionId,
            MultipartFile audio
    );
}
package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import com.example.mofawid.DTO.ObservationDTO.TranscriptionResponseDTO;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.services.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.example.mofawid.mapper.ObservationMapper;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Observation;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.ObservationService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.exceptions.ForbiddenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ObservationServiceImpl implements ObservationService {

    // a recording of about 5 minutes is less than 5 MB, 15 MB is a safe limit
    private static final long MAX_AUDIO_SIZE = 15L * 1024 * 1024;

    private final ObservationRepo observationRepository;
    private final MissionRepo missionRepository;
    private final UserRepo userRepository;
    private final ObservationMapper observationMapper;
    private final AiService aiService;

    public ObservationServiceImpl(
            ObservationRepo observationRepository,
            MissionRepo missionRepository,
            UserRepo userRepository,
            ObservationMapper observationMapper,
            AiService aiService
    ) {
        this.observationRepository = observationRepository;
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.observationMapper = observationMapper;
        this.aiService = aiService;
    }

    @Transactional
    @Override
    public ObservationResponseDTO createObservation(
            UUID missionId,
            ObservationRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Check mission belongs to current tenant
        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        missionId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        // Collaborateur can add observation
        // only to his own mission
        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        Observation observation =
                observationMapper.toEntity(dto);

        observation.setMission(mission);

        Observation savedObservation =
                observationRepository.save(observation);

        return observationMapper.toResponseDto(
                savedObservation
        );
    }

    @Override
    public Page<ObservationResponseDTO> getMissionObservations(
            UUID missionId,
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();


        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        missionId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        // Collaborateur can only see observations
        // of a mission assigned to him
        if (currentUser.getRole() == Role.COLLABORATEUR
                && !mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Observation> observations =
                observationRepository
                        .findAllByMissionId(
                                missionId,
                                pageable
                        );

        return observations.map(
                observationMapper::toResponseDto
        );
    }

    @Override
    public TranscriptionResponseDTO transcribeAudio(
            UUID missionId,
            MultipartFile audio
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Check mission belongs to current tenant
        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        missionId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        // same rule as createObservation: only the collaborateur of the mission
        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        if (audio == null || audio.isEmpty()) {
            throw new BadRequestException("The recording is empty");
        }

        if (audio.getSize() > MAX_AUDIO_SIZE) {
            throw new BadRequestException("The recording is too long (max 15 MB)");
        }

        // "audio/webm;codecs=opus" -> "audio/webm"
        String mimeType = audio.getContentType();
        if (mimeType != null && mimeType.contains(";")) {
            mimeType = mimeType.substring(0, mimeType.indexOf(";"));
        }
        if (mimeType == null || !mimeType.trim().startsWith("audio/")) {
            throw new BadRequestException("Only audio files can be transcribed");
        }

        byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the recording");
        }

        String text;
        try {
            text = aiService.transcribe(bytes, mimeType.trim());
        } catch (RuntimeException e) {
            log.error("Audio transcription failed for mission {}", missionId, e);
            throw new BadRequestException("Could not transcribe the recording, please try again");
        }

        // nobody spoke = empty text (the front shows a message)
        return new TranscriptionResponseDTO(text == null ? "" : text.trim());
    }

    private User getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current user not found"
                        )
                );
    }
}
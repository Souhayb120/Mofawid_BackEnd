package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.mapper.ObservationMapper;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Observation;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.ObservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ObservationServiceImpl implements ObservationService {

    private final ObservationRepo observationRepository;
    private final MissionRepo missionRepository;
    private final UserRepo userRepository;
    private final ObservationMapper observationMapper;

    public ObservationServiceImpl(
            ObservationRepo observationRepository,
            MissionRepo missionRepository,
            UserRepo userRepository,
            ObservationMapper observationMapper
    ) {
        this.observationRepository = observationRepository;
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.observationMapper = observationMapper;
    }

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
                        new RuntimeException("Mission not found")
                );

        // Collaborateur can add observation
        // only to his own mission
        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
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
                        new RuntimeException("Mission not found")
                );

        // Collaborateur can only see observations
        // of a mission assigned to him
        if (currentUser.getRole() == Role.COLLABORATEUR
                && !mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
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

    private User getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Current user not found"
                        )
                );
    }
}
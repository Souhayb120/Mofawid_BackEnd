package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptRequestDTO;
import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptResponseDTO;
import com.example.mofawid.Enums.DeliveryResult;
import com.example.mofawid.Enums.MissionStatus;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.exceptions.ForbiddenException;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.mapper.DeliveryAttemptMapper;
import com.example.mofawid.models.DeliveryAttempt;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.DeliveryAttemptRepo;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.DeliveryAttemptService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryAttemptServiceImpl implements DeliveryAttemptService {

    private final DeliveryAttemptRepo deliveryAttemptRepository;
    private final MissionRepo missionRepository;
    private final UserRepo userRepository;
    private final DeliveryAttemptMapper deliveryAttemptMapper;

    public DeliveryAttemptServiceImpl(
            DeliveryAttemptRepo deliveryAttemptRepository,
            MissionRepo missionRepository,
            UserRepo userRepository,
            DeliveryAttemptMapper deliveryAttemptMapper
    ) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.deliveryAttemptMapper = deliveryAttemptMapper;
    }

    @Transactional
    @Override
    public DeliveryAttemptResponseDTO addAttempt(
            UUID missionId,
            DeliveryAttemptRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(missionId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        // only the collaborateur of the mission can add a visit
        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        if (mission.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Start the mission before adding a visit"
            );
        }

        // when someone was found, we need his name to write the report
        if ((dto.getResult() == DeliveryResult.DELIVERED_TO_OTHER
                || dto.getResult() == DeliveryResult.REFUSED)
                && isBlank(dto.getReceiverName())) {

            throw new BadRequestException(
                    "The name of the person found is required"
            );
        }

        // ex: "زوجته", "والده"
        if (dto.getResult() == DeliveryResult.DELIVERED_TO_OTHER
                && isBlank(dto.getReceiverCapacity())) {

            throw new BadRequestException(
                    "The capacity of the person who received is required"
            );
        }

        DeliveryAttempt attempt =
                deliveryAttemptMapper.toEntity(dto);

        attempt.setMission(mission);

        if (attempt.getAttemptedAt() == null) {
            attempt.setAttemptedAt(LocalDateTime.now());
        }

        DeliveryAttempt savedAttempt =
                deliveryAttemptRepository.save(attempt);

        return deliveryAttemptMapper.toResponseDto(savedAttempt);
    }

    @Override
    public List<DeliveryAttemptResponseDTO> getMissionAttempts(UUID missionId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(missionId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        // Collaborateur can only see the visits of his own mission
        if (currentUser.getRole() == Role.COLLABORATEUR
                && !mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        List<DeliveryAttempt> attempts =
                deliveryAttemptRepository
                        .findAllByMissionIdOrderByAttemptedAtAsc(missionId);

        return attempts.stream()
                .map(deliveryAttemptMapper::toResponseDto)
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.MissionDTO.MissionPresenceDTO;
import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.MissionStatus;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.mapper.MissionMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.MissionService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.exceptions.ForbiddenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MissionServiceImpl implements MissionService {

    private final MissionRepo missionRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final MissionMapper missionMapper;

    public MissionServiceImpl(
            MissionRepo missionRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            MissionMapper missionMapper
    ) {
        this.missionRepository = missionRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.missionMapper = missionMapper;
    }
    @Transactional
    @Override
    public MissionResponseDTO createMission(MissionRequestDTO dto) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(dto.getCaseId(), tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        User collaborateur = userRepository
                .findByIdAndTenantId(
                        dto.getCollaborateurId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Collaborateur not found")
                );

        if (collaborateur.getRole() != Role.COLLABORATEUR) {
            throw new BadRequestException(
                    "Selected user must be a COLLABORATEUR"
            );
        }

        Mission mission =
                missionMapper.toEntity(dto);

        mission.setCaseEntity(caseEntity);
        mission.setAssignedTo(collaborateur);
        mission.setAssignedBy(currentUser);
        mission.setStatus(MissionStatus.ASSIGNED);

        // Update Case status
        caseEntity.setCaseStatus(
                CaseStatus.ASSIGNED
        );

        caseRepository.save(caseEntity);

        Mission savedMission =
                missionRepository.save(mission);

        return missionMapper.toResponseDto(
                savedMission
        );
    }

    @Override
    public MissionResponseDTO getMissionById(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        if (currentUser.getRole() == Role.COLLABORATEUR
                && !mission.getAssignedTo().getId().equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        return missionMapper.toResponseDto(mission);
    }

    @Override
    public Page<MissionResponseDTO> getAllMissions(
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Mission> missions =
                missionRepository.findAllByCaseEntityTenantId(
                        tenantId,
                        pageable
                );

        return missions.map(
                missionMapper::toResponseDto
        );
    }

    @Override
    public Page<MissionResponseDTO> getMyMissions(
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Mission> missions =
                missionRepository.findAllByAssignedToId(
                        currentUser.getId(),
                        pageable
                );

        return missions.map(
                missionMapper::toResponseDto
        );
    }

    @Transactional
    @Override
    public MissionResponseDTO startMission(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        if (mission.getStatus()
                != MissionStatus.ASSIGNED) {

            throw new BadRequestException(
                    "Only an ASSIGNED mission can be started"
            );
        }

        mission.setStatus(
                MissionStatus.IN_PROGRESS
        );

        // Update the Case too
        Case caseEntity =
                mission.getCaseEntity();

        caseEntity.setCaseStatus(
                CaseStatus.IN_PROGRESS
        );

        caseRepository.save(caseEntity);

        Mission updatedMission =
                missionRepository.save(mission);

        return missionMapper.toResponseDto(
                updatedMission
        );
    }

    @Transactional
    @Override
    public MissionResponseDTO completeMission(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        if (mission.getStatus() != MissionStatus.IN_PROGRESS) {

            throw new BadRequestException(
                    "Only an IN_PROGRESS mission can be completed"
            );
        }

        mission.setStatus(
                MissionStatus.COMPLETED
        );

        Mission updatedMission =
                missionRepository.save(mission);

        return missionMapper.toResponseDto(
                updatedMission
        );
    }

    @Transactional
    @Override
    public MissionResponseDTO updatePresence(
            UUID id,
            MissionPresenceDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Mission mission = missionRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mission not found")
                );

        if (!mission.getAssignedTo()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not assigned to this mission"
            );
        }

        if (mission.getStatus() != MissionStatus.IN_PROGRESS) {

            throw new BadRequestException(
                    "Start the mission before adding the person present"
            );
        }

        mission.setPresentPersonName(dto.getPresentPersonName());
        mission.setPresentPersonCapacity(dto.getPresentPersonCapacity());

        Mission updatedMission =
                missionRepository.save(mission);

        return missionMapper.toResponseDto(
                updatedMission
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
                        new ResourceNotFoundException(
                                "Current user not found"
                        )
                );
    }
}
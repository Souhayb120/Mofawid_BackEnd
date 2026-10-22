package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.PartyDTO.PartyRequestDTO;
import com.example.mofawid.DTO.PartyDTO.PartyResponseDTO;
import com.example.mofawid.Enums.PartyType;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.mapper.PartyMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Party;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.PartyRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.PartyService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PartyServiceImpl implements PartyService {

    private final PartyRepo partyRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final PartyMapper partyMapper;

    public PartyServiceImpl(
            PartyRepo partyRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            PartyMapper partyMapper
    ) {
        this.partyRepository = partyRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.partyMapper = partyMapper;
    }

    @Transactional
    @Override
    public PartyResponseDTO addParty(
            UUID caseId,
            PartyRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Case must belong to current tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        checkParty(dto);

        Party party =
                partyMapper.toEntity(dto);

        party.setCaseEntity(caseEntity);

        Party savedParty =
                partyRepository.save(party);

        return partyMapper.toResponseDto(savedParty);
    }

    @Override
    public List<PartyResponseDTO> getCaseParties(UUID caseId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // verify the Case itself belongs to current tenant
        caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        List<Party> parties =
                partyRepository.findAllByCaseEntityId(caseId);

        return parties.stream()
                .map(partyMapper::toResponseDto)
                .toList();
    }

    @Transactional
    @Override
    public PartyResponseDTO updateParty(
            UUID id,
            PartyRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Party party = partyRepository
                .findByIdAndCaseEntityTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Party not found")
                );

        checkParty(dto);

        partyMapper.updateEntityFromDto(dto, party);

        Party updatedParty =
                partyRepository.save(party);

        return partyMapper.toResponseDto(updatedParty);
    }

    @Transactional
    @Override
    public void deleteParty(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Party party = partyRepository
                .findByIdAndCaseEntityTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Party not found")
                );

        partyRepository.delete(party);
    }

    // a company must have its RC or its legal representative
    private void checkParty(PartyRequestDTO dto) {

        if (dto.getType() == PartyType.COMPANY
                && isBlank(dto.getRc())
                && isBlank(dto.getLegalRepresentative())) {

            throw new BadRequestException(
                    "A company needs its RC or its legal representative"
            );
        }
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

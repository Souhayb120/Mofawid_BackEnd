package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.mapper.CaseMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.PartyRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.CaseService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
public class CaseServiceImpl implements CaseService {

    private final CaseRepo caseRepository;
    private final CaseMapper caseMapper;
    private final UserRepo userRepository;
    private final PartyRepo partyRepository;

    public CaseServiceImpl(
            CaseRepo caseRepository,
            CaseMapper caseMapper,
            UserRepo userRepository,
            PartyRepo partyRepository
    ) {
        this.caseRepository = caseRepository;
        this.caseMapper = caseMapper;
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
    }

    @Transactional
    @Override
    public CaseResponseDTO createCase(CaseRequestDTO dto) {

        User currentUser = getCurrentUser();

        Case caseEntity = caseMapper.toEntity(dto);

        caseEntity.setTenant(currentUser.getTenant());
        caseEntity.setCreatedBy(currentUser);
        caseEntity.setCaseStatus(CaseStatus.QUALIFIED);

        // مرجعنا: the next number of the office register for this year (ex: 2026/2282)
        Tenant tenant = currentUser.getTenant();

        int year = Year.now().getValue();

        Integer max = caseRepository.findMaxRegisterNumber(
                tenant.getId(),
                year
        );

        int number;

        if (max != null) {
            number = max + 1;
        } else if (tenant.getStartNumber() != null) {
            // first case in the app: continue from the paper register
            number = tenant.getStartNumber();
        } else {
            number = 1;
        }

        caseEntity.setRegisterYear(year);
        caseEntity.setRegisterNumber(number);
        caseEntity.setRef(year + "/" + number);

        Case savedCase =
                caseRepository.save(caseEntity);

        return caseMapper.toResponseDto(savedCase);
    }

    @Override
    public CaseResponseDTO getCaseById(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        return caseMapper.toResponseDto(caseEntity);
    }

    @Override
    public Page<CaseResponseDTO> getAllCases(
            int page,
            int size,
            String search,
            CaseStatus status
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Pageable pageable =
                PageRequest.of(page, size);

        // empty search = no filter, else "%text%" for the LIKE
        String searchPattern = null;
        if (search != null && !search.isBlank()) {
            searchPattern = "%" + search.trim().toLowerCase() + "%";
        }

        Page<Case> cases =
                caseRepository.searchCases(
                        tenantId,
                        status,
                        searchPattern,
                        pageable
                );

        return cases.map(
                caseMapper::toResponseDto
        );
    }

    @Transactional
    @Override
    public CaseResponseDTO updateCase(
            UUID id,
            CaseRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        caseEntity.setClientName(dto.getClientName());
        caseEntity.setClientIce(dto.getClientIce());
        caseEntity.setAddress(dto.getAddress());
        caseEntity.setPhone(dto.getPhone());
        caseEntity.setSubject(dto.getSubject());
        caseEntity.setDescription(dto.getDescription());
        caseEntity.setDeadline(dto.getDeadline());

        Case updatedCase =
                caseRepository.save(caseEntity);

        return caseMapper.toResponseDto(updatedCase);
    }

    @Transactional
    @Override
    public void deleteCase(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        // delete the parties of the case first (foreign key)
        partyRepository.deleteAll(
                partyRepository.findAllByCaseEntityId(id)
        );

        caseRepository.delete(caseEntity);
    }

    private User getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Current user not found")
                );
    }
}
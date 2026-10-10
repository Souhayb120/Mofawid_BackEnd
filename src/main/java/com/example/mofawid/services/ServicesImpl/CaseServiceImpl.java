package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.mapper.CaseMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.CaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

@Service
public class CaseServiceImpl implements CaseService {

    private final CaseRepo caseRepository;
    private final CaseMapper caseMapper;
    private final UserRepo userRepository;

    public CaseServiceImpl(
            CaseRepo caseRepository,
            CaseMapper caseMapper,
            UserRepo userRepository
    ) {
        this.caseRepository = caseRepository;
        this.caseMapper = caseMapper;
        this.userRepository = userRepository;
    }

    @Override
    public CaseResponseDTO createCase(CaseRequestDTO dto) {

        User currentUser = getCurrentUser();

        Case caseEntity = caseMapper.toEntity(dto);

        caseEntity.setTenant(currentUser.getTenant());
        caseEntity.setCreatedBy(currentUser);
        caseEntity.setCaseStatus(CaseStatus.QUALIFIED);

        caseEntity.setRef(
                "CASE-" + Year.now().getValue() + "-" +
                        UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

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
                        new RuntimeException("Case not found")
                );

        return caseMapper.toResponseDto(caseEntity);
    }

    @Override
    public Page<CaseResponseDTO> getAllCases(
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Case> cases =
                caseRepository.findAllByTenantId(
                        tenantId,
                        pageable
                );

        return cases.map(
                caseMapper::toResponseDto
        );
    }

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
                        new RuntimeException("Case not found")
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

    @Override
    public void deleteCase(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Case not found")
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
                        new RuntimeException("Current user not found")
                );
    }
}
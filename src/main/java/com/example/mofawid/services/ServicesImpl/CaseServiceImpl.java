package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.mapper.CaseMapper;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.CaseService;
import com.example.mofawid.services.StorageService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {

    private final CaseRepo caseRepository;
    private final CaseMapper caseMapper;
    private final UserRepo userRepository;
    private final PartyRepo partyRepository;
    private final MissionRepo missionRepository;
    private final ObservationRepo observationRepository;
    private final DeliveryAttemptRepo deliveryAttemptRepository;
    private final AttachmentRepository attachmentRepository;
    private final DocumentRepo documentRepository;
    private final BillingRepo billingRepository;
    private final CaseTrackingRepo caseTrackingRepository;
    private final StorageService storageService;

    public CaseServiceImpl(
            CaseRepo caseRepository,
            CaseMapper caseMapper,
            UserRepo userRepository,
            PartyRepo partyRepository,
            MissionRepo missionRepository,
            ObservationRepo observationRepository,
            DeliveryAttemptRepo deliveryAttemptRepository,
            AttachmentRepository attachmentRepository,
            DocumentRepo documentRepository,
            BillingRepo billingRepository,
            CaseTrackingRepo caseTrackingRepository,
            StorageService storageService
    ) {
        this.caseRepository = caseRepository;
        this.caseMapper = caseMapper;
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
        this.missionRepository = missionRepository;
        this.observationRepository = observationRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.attachmentRepository = attachmentRepository;
        this.documentRepository = documentRepository;
        this.billingRepository = billingRepository;
        this.caseTrackingRepository = caseTrackingRepository;
        this.storageService = storageService;
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

        // an approved document is an official PV: the case must be kept
        if (documentRepository.existsByCaseEntityIdAndStatus(id, DocumentStatus.APPROVED)) {
            throw new BadRequestException("This case has an approved document, it can not be deleted");
        }

        // delete the children first (foreign keys), the deepest ones first

        // 1. the files (mission files too: they all have the case)
        List<Attachment> attachments =
                attachmentRepository.findAllByCaseEntityIdOrderByUploadedAtAsc(id);
        attachmentRepository.deleteAll(attachments);

        // 2. the missions with their visits and observations
        List<Mission> missions = missionRepository.findAllByCaseEntityId(id);
        for (Mission mission : missions) {
            observationRepository.deleteAll(
                    observationRepository.findAllByMissionId(mission.getId())
            );
            deliveryAttemptRepository.deleteAll(
                    deliveryAttemptRepository.findAllByMissionIdOrderByAttemptedAtAsc(mission.getId())
            );
        }
        missionRepository.deleteAll(missions);

        // 3. the documents, the billing, the tracking code and the parties
        documentRepository.deleteAll(documentRepository.findAllByCaseEntityId(id));
        billingRepository.findByCaseEntityId(id).ifPresent(billingRepository::delete);
        caseTrackingRepository.findByCaseEntityId(id).ifPresent(caseTrackingRepository::delete);
        partyRepository.deleteAll(
                partyRepository.findAllByCaseEntityId(id)
        );

        caseRepository.delete(caseEntity);

        // 4. the files in the storage (R2): if one fails, the case is still deleted
        for (Attachment attachment : attachments) {
            try {
                storageService.delete(attachment.getStorageKey());
            } catch (Exception e) {
                log.warn("File {} could not be deleted from the storage: {}", attachment.getStorageKey(), e.getMessage());
            }
        }
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
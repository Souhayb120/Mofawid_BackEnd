package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.Prompt.DocumentPromptFactory;
import com.example.mofawid.Enums.DeliveryResult;
import com.example.mofawid.Enums.PartyRole;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.DeliveryAttempt;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Party;
import com.example.mofawid.models.Observation;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.services.AiService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.mapper.DocumentMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.DocumentService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final MissionRepo missionRepository;
    private final ObservationRepo observationRepository;
    private final AiService aiService;
    private final DocumentRepo documentRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final DocumentMapper documentMapper;
    private final DocumentPromptFactory documentPromptFactory;
    private final PartyRepo partyRepository;
    private final DeliveryAttemptRepo deliveryAttemptRepository;
    private final AttachmentRepository attachmentRepository;

    public DocumentServiceImpl(
            MissionRepo missionRepository, ObservationRepo observationRepository, AiService aiService, DocumentRepo documentRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            DocumentMapper documentMapper, DocumentPromptFactory documentPromptFactory,
            PartyRepo partyRepository,
            DeliveryAttemptRepo deliveryAttemptRepository,
            AttachmentRepository attachmentRepository
    ) {
        this.partyRepository = partyRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.attachmentRepository = attachmentRepository;
        this.missionRepository = missionRepository;
        this.observationRepository = observationRepository;
        this.aiService = aiService;
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
        this.documentPromptFactory = documentPromptFactory;
    }

    @Transactional
    @Override
    public DocumentResponseDTO createDocument(
            UUID caseId,
            DocumentRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Case must belong to current tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        Document document =
                documentMapper.toEntity(dto);

        document.setCaseEntity(caseEntity);

        document.setStatus(
                DocumentStatus.DRAFT
        );

        Document savedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                savedDocument
        );
    }

    @Override
    public DocumentResponseDTO getDocumentById(
            UUID id
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        return documentMapper.toResponseDto(document);
    }

    @Override
    public Page<DocumentResponseDTO> getCaseDocuments(
            UUID caseId,
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Important:
        // verify the Case itself belongs to current tenant
        caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Document> documents =
                documentRepository
                        .findAllByCaseEntityId(
                                caseId,
                                pageable
                        );

        return documents.map(
                documentMapper::toResponseDto
        );
    }

    @Transactional
    @Override
    public DocumentResponseDTO updateDocument(
            UUID id,
            DocumentRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new BadRequestException(
                    "Approved document cannot be modified"
            );
        }

        document.setKind(dto.getKind());
        document.setContent(dto.getContent());

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }

    @Transactional
    @Override
    public void deleteDocument(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new BadRequestException(
                    "Approved document cannot be deleted"
            );
        }

        documentRepository.delete(document);
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



    @Override
    public DocumentResponseDTO generateAiDraft(
            UUID caseId,
            DocumentKind kind
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        List<Mission> missions =
                missionRepository.findAllByCaseEntityId(caseId);

        List<Observation> observations = missions.stream()
                .flatMap(mission ->
                        observationRepository
                                .findAllByMissionId(mission.getId())
                                .stream()
                )
                .toList();

        List<Party> parties =
                partyRepository.findAllByCaseEntityId(caseId);

        // all the visits of all the missions, in the order they happened
        List<DeliveryAttempt> attempts = new ArrayList<>();
        for (Mission mission : missions) {
            attempts.addAll(
                    deliveryAttemptRepository
                            .findAllByMissionIdOrderByAttemptedAtAsc(mission.getId())
            );
        }
        attempts.sort(Comparator.comparing(DeliveryAttempt::getAttemptedAt));

        List<Attachment> attachments =
                attachmentRepository.findAllByCaseEntityIdOrderByUploadedAtAsc(caseId);

        checkDataForKind(kind, parties, attempts, observations);

        String prompt = documentPromptFactory.buildPrompt(
                kind,
                caseEntity,
                missions,
                observations,
                parties,
                attempts,
                attachments
        );


        String generatedText = aiService.generate(prompt);

        Document document = new Document();

        document.setCaseEntity(caseEntity);
        document.setKind(kind);
        document.setStatus(DocumentStatus.DRAFT_AI);
        document.setContent(generatedText);

        Document savedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(savedDocument);
    }

    // stop early with a clear message when the data does not match the type of report
    private void checkDataForKind(
            DocumentKind kind,
            List<Party> parties,
            List<DeliveryAttempt> attempts,
            List<Observation> observations
    ) {

        boolean isNotification = kind == DocumentKind.NOTIFICATION
                || kind == DocumentKind.NOTIFICATION_INFORMATIF
                || kind == DocumentKind.NOTIFICATION_REFUS;

        if (!isNotification) {

            // all the constats are written from the observations
            if (observations.isEmpty()) {
                throw new BadRequestException(
                        "Add the observations of the mission before generating a constat"
                );
            }

            return;
        }

        boolean hasTarget = parties.stream()
                .anyMatch(party -> party.getRole() == PartyRole.TARGET);

        if (!hasTarget) {
            throw new BadRequestException(
                    "Add the person to notify (party with role TARGET) first"
            );
        }

        if (attempts.isEmpty()) {
            throw new BadRequestException(
                    "Add the visits of the mission before generating a notification"
            );
        }

        // the type of notification depends on the result of the last visit
        DeliveryResult lastResult =
                attempts.get(attempts.size() - 1).getResult();

        DocumentKind expectedKind;

        if (lastResult == DeliveryResult.DELIVERED_IN_PERSON
                || lastResult == DeliveryResult.DELIVERED_TO_OTHER) {
            expectedKind = DocumentKind.NOTIFICATION;
        } else if (lastResult == DeliveryResult.REFUSED) {
            expectedKind = DocumentKind.NOTIFICATION_REFUS;
        } else {
            expectedKind = DocumentKind.NOTIFICATION_INFORMATIF;
        }

        if (kind != expectedKind) {
            throw new BadRequestException(
                    "The last visit result is " + lastResult
                            + ", the report type must be " + expectedKind
            );
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "[المعلومة غير متوفرة]"
                : value;
    }

    @Transactional
    @Override
    public DocumentResponseDTO submitForApproval(UUID documentId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        documentId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        if (document.getStatus() != DocumentStatus.DRAFT
                && document.getStatus() != DocumentStatus.DRAFT_AI) {

            throw new BadRequestException(
                    "Only a draft document can be submitted for approval"
            );
        }

        document.setStatus(
                DocumentStatus.AWAITING_APPROVAL
        );

        Case caseEntity = document.getCaseEntity();

        caseEntity.setCaseStatus(
                CaseStatus.AWAITING_APPROVAL
        );

        caseRepository.save(caseEntity);

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }

    @Transactional
    @Override
    public DocumentResponseDTO approveDocument(UUID documentId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        documentId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        if (document.getStatus()
                != DocumentStatus.AWAITING_APPROVAL) {

            throw new BadRequestException(
                    "Document must be awaiting approval"
            );
        }

        document.setStatus(
                DocumentStatus.APPROVED
        );

        document.setApprovedAt(Instant.now());
        document.setApprovedBy(currentUser);

        Case caseEntity = document.getCaseEntity();

        caseEntity.setCaseStatus(
                CaseStatus.CLOSED
        );

        caseRepository.save(caseEntity);

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }
}
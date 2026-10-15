package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.Prompt.DocumentPromptFactory;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Observation;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.services.AiService;
import java.util.List;
import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.mapper.DocumentMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    public DocumentServiceImpl(
            MissionRepo missionRepository, ObservationRepo observationRepository, AiService aiService, DocumentRepo documentRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            DocumentMapper documentMapper, DocumentPromptFactory documentPromptFactory
    ) {
        this.missionRepository = missionRepository;
        this.observationRepository = observationRepository;
        this.aiService = aiService;
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
        this.documentPromptFactory = documentPromptFactory;
    }

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
                        new RuntimeException("Case not found")
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
                        new RuntimeException("Document not found")
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
                        new RuntimeException("Case not found")
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
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new RuntimeException(
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
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new RuntimeException(
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
                        new RuntimeException(
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
                        new RuntimeException("Case not found")
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

        String prompt = documentPromptFactory.buildPrompt(
                kind,
                caseEntity,
                missions,
                observations
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

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "[المعلومة غير متوفرة]"
                : value;
    }

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
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() != DocumentStatus.DRAFT
                && document.getStatus() != DocumentStatus.DRAFT_AI) {

            throw new RuntimeException(
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
                        new RuntimeException("Document not found")
                );

        if (document.getStatus()
                != DocumentStatus.AWAITING_APPROVAL) {

            throw new RuntimeException(
                    "Document must be awaiting approval"
            );
        }

        document.setStatus(
                DocumentStatus.APPROVED
        );

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
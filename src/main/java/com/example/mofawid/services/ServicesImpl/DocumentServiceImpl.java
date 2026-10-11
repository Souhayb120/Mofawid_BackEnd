package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
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

    public DocumentServiceImpl(
            MissionRepo missionRepository, ObservationRepo observationRepository, AiService aiService, DocumentRepo documentRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            DocumentMapper documentMapper
    ) {
        this.missionRepository = missionRepository;
        this.observationRepository = observationRepository;
        this.aiService = aiService;
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
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
    public DocumentResponseDTO generateAiDraft(UUID caseId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // 1. Get Case from same tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Case not found")
                );

        // 2. Get all missions of this case
        List<Mission> missions =
                missionRepository.findAllByCaseEntityId(caseId);

        // 3. Build prompt
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are helping draft a professional legal document.

            IMPORTANT RULES:
            - Use only the information provided below.
            - Do not invent facts.
            - Clearly distinguish direct observations from declarations.
            - If information is missing, do not guess.
            - Produce a draft that must be reviewed by the administrator.

            CASE INFORMATION:
            """);

        prompt.append("\nReference: ")
                .append(caseEntity.getRef());

        prompt.append("\nClient: ")
                .append(caseEntity.getClientName());

        prompt.append("\nICE: ")
                .append(caseEntity.getClientIce());

        prompt.append("\nAddress: ")
                .append(caseEntity.getAddress());

        prompt.append("\nSubject: ")
                .append(caseEntity.getSubject());

        prompt.append("\nDescription: ")
                .append(caseEntity.getDescription());

        prompt.append("\n\nMISSIONS AND OBSERVATIONS:\n");

        // 4. Add missions + observations
        for (Mission mission : missions) {

            prompt.append("\nMission: ")
                    .append(mission.getTitle());

            prompt.append("\nMission description: ")
                    .append(mission.getDescription());

            List<Observation> observations =
                    observationRepository.findAllByMissionId(
                            mission.getId()
                    );

            for (Observation observation : observations) {

                prompt.append("\n- ")
                        .append(observation.getKind())
                        .append(": ")
                        .append(observation.getDescription());
            }

            prompt.append("\n");
        }

        // 5. Ask Gemini
        String generatedText =
                aiService.generate(prompt.toString());

        // 6. Create AI Document
        Document document = new Document();

        document.setCaseEntity(caseEntity);

        document.setKind(
                DocumentKind.PROCES_VERBAL
        );

        document.setStatus(
                DocumentStatus.DRAFT_AI
        );

        document.setContent(generatedText);

        Document savedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                savedDocument
        );
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
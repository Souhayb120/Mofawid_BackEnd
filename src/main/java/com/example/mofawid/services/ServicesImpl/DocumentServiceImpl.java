package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.mapper.DocumentMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.DocumentRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepo documentRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(
            DocumentRepo documentRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            DocumentMapper documentMapper
    ) {
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
}
package com.example.mofawid.services;

import com.example.mofawid.DTO.AttachmentDTO.AttachmentDownloadDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.Enums.DocumentKind;
import org.springframework.data.domain.Page;

import java.util.UUID;
public interface DocumentService {

    DocumentResponseDTO createDocument(
            UUID caseId,
            DocumentRequestDTO dto
    );

    DocumentResponseDTO getDocumentById(UUID id);

    Page<DocumentResponseDTO> getCaseDocuments(
            UUID caseId,
            int page,
            int size
    );

    DocumentResponseDTO updateDocument(
            UUID id,
            DocumentRequestDTO dto
    );

    void deleteDocument(UUID id);

    DocumentResponseDTO generateAiDraft(
            UUID caseId,
            DocumentKind kind
    );


    DocumentResponseDTO submitForApproval(UUID documentId);

    DocumentResponseDTO approveDocument(UUID documentId);

    // the official PDF of an approved document (letterhead + text + photos)
    AttachmentDownloadDTO downloadPdf(UUID documentId);
}
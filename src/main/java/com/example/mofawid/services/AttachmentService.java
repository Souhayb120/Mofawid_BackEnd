package com.example.mofawid.services;


import com.example.mofawid.DTO.AttachmentDTO.AttachmentDownloadDTO;
import com.example.mofawid.DTO.AttachmentDTO.AttachmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AttachmentService {

    AttachmentResponseDTO uploadToCase(
            UUID caseId,
            MultipartFile file,
            String caption
    );

    Page<AttachmentResponseDTO> getCaseAttachments(
            UUID caseId,
            Pageable pageable
    );

    AttachmentDownloadDTO downloadAttachment(UUID attachmentId);

    void deleteAttachment(UUID attachmentId);
}
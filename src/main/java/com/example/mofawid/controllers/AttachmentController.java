package com.example.mofawid.controllers;

import com.example.mofawid.DTO.AttachmentDTO.AttachmentDownloadDTO;
import com.example.mofawid.DTO.AttachmentDTO.AttachmentResponseDTO;
import com.example.mofawid.services.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/api/cases/{caseId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<AttachmentResponseDTO> uploadToCase(
            @PathVariable UUID caseId,
            @RequestParam("file") MultipartFile file,
            // optional text under the photo in the report
            @RequestParam(value = "caption", required = false) String caption
    ) {

        AttachmentResponseDTO response =
                attachmentService.uploadToCase(caseId, file, caption);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/cases/{caseId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<Page<AttachmentResponseDTO>> getCaseAttachments(
            @PathVariable UUID caseId,
            Pageable pageable
    ) {

        Page<AttachmentResponseDTO> attachments =
                attachmentService.getCaseAttachments(caseId, pageable);

        return ResponseEntity.ok(attachments);
    }



    @GetMapping("/api/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID attachmentId,
            @RequestParam(name = "inline", defaultValue = "false") boolean inline
    ) {

        AttachmentDownloadDTO file =
                attachmentService.downloadAttachment(attachmentId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename =
                (file.originalName() == null || file.originalName().isBlank())
                        ? "attachment"
                        : file.originalName();

        ContentDisposition disposition = ContentDisposition
                .builder(inline ? "inline" : "attachment")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId
    ) {

        attachmentService.deleteAttachment(attachmentId);

        return ResponseEntity.noContent().build();
    }
}
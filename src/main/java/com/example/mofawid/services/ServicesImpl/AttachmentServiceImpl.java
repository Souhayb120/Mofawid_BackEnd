package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.AttachmentDTO.AttachmentDownloadDTO;
import com.example.mofawid.DTO.AttachmentDTO.AttachmentResponseDTO;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.mapper.AttachmentMapper;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.AttachmentRepository;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.AttachmentService;
import com.example.mofawid.services.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final UserRepo userRepo;
    private final CaseRepo caseRepo;
    private final StorageService storageService;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;

    public AttachmentServiceImpl(UserRepo userRepo, CaseRepo caseRepo, StorageService storageService, AttachmentRepository attachmentRepository, AttachmentMapper attachmentMapper) {
        this.userRepo = userRepo;
        this.caseRepo = caseRepo;
        this.storageService = storageService;
        this.attachmentRepository = attachmentRepository;
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public AttachmentResponseDTO uploadToCase(UUID caseId, MultipartFile file) {
        User currentUser = getLoggedUser();
        Tenant tenant = currentUser.getTenant();
        UUID currentTenantId = currentUser.getTenant().getId();
        Case currentCase = caseRepo.findByIdAndTenantId(caseId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("case not found !! "));
        if (file.isEmpty()) {
            throw new RuntimeException("the file is emtpty !!");
        }
            String originalName = file.getOriginalFilename();
            String uniqueName = UUID.randomUUID() + "-" + originalName;
            String storageKey =
                    "tenants/" + currentTenantId +
                            "/cases/" + caseId +
                            "/" + uniqueName;

            storageService.upload(file,storageKey);
        Attachment attachment = new Attachment();
        attachment.setCaseEntity(currentCase);
        attachment.setStorageKey(storageKey);
        attachment.setUploadedBy(currentUser);
        attachment.setOriginalName(originalName);
        attachment.setTenant(tenant);
        attachment.setSize(file.getSize());
        attachment.setContentType(file.getContentType());
        Attachment saved = attachmentRepository.save(attachment);

        return attachmentMapper.toResponseDto(saved);
    }

    @Override
    public Page<AttachmentResponseDTO> getCaseAttachments(UUID caseId, Pageable pageable) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Case currentCase = caseRepo.findByIdAndTenantId(caseId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("case not found !! "));

        Page<Attachment> caseAttachments = attachmentRepository.findAllByCaseEntityIdAndTenantId(currentCase.getId(),currentTenantId,pageable);

        return caseAttachments.map(attachmentMapper::toResponseDto);
    }

    @Override
    public AttachmentDownloadDTO downloadAttachment(UUID attachmentId) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, currentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found !! "));

        byte[] content = storageService.download(attachment.getStorageKey());

        return new AttachmentDownloadDTO(
                content,
                attachment.getContentType(),
                attachment.getOriginalName()
        );
    }

    @Override
    public void deleteAttachment(UUID attachmentId) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Attachment currentAttachment = attachmentRepository.findByIdAndTenantId(attachmentId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("Attachment not found !! "));
        storageService.delete(currentAttachment.getStorageKey());
        attachmentRepository.delete(currentAttachment);
    }


    public User getLoggedUser() {
        String userName = SecurityContextHolder.
                getContext()
                .getAuthentication()
                .getName();
        return userRepo
                .findByUsername(userName)
                .orElseThrow(
                        () -> new ResourceNotFoundException("user not found !! "));
    }
}

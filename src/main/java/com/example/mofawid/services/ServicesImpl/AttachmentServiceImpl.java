package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.AttachmentDTO.AttachmentDownloadDTO;
import com.example.mofawid.DTO.AttachmentDTO.AttachmentResponseDTO;
import com.example.mofawid.Enums.MissionStatus;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.exceptions.ForbiddenException;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.mapper.AttachmentMapper;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.AttachmentRepository;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.AttachmentService;
import com.example.mofawid.services.StorageService;
import com.example.mofawid.exceptions.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final UserRepo userRepo;
    private final CaseRepo caseRepo;
    private final StorageService storageService;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final MissionRepo missionRepo;

    public AttachmentServiceImpl(UserRepo userRepo, CaseRepo caseRepo, StorageService storageService, AttachmentRepository attachmentRepository, AttachmentMapper attachmentMapper, MissionRepo missionRepo) {
        this.userRepo = userRepo;
        this.caseRepo = caseRepo;
        this.storageService = storageService;
        this.attachmentRepository = attachmentRepository;
        this.attachmentMapper = attachmentMapper;
        this.missionRepo = missionRepo;
    }

    @Transactional
    @Override
    public AttachmentResponseDTO uploadToCase(UUID caseId, MultipartFile file, String caption) {
        User currentUser = getLoggedUser();
        Tenant tenant = currentUser.getTenant();
        UUID currentTenantId = currentUser.getTenant().getId();
        Case currentCase = caseRepo.findByIdAndTenantId(caseId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("case not found !! "));
        if (file.isEmpty()) {
            throw new BadRequestException("the file is emtpty !!");
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
        attachment.setCaption(caption);
        Attachment saved;
        try {
            // saveAndFlush so a DB error happens here and not later at commit
            saved = attachmentRepository.saveAndFlush(attachment);
        } catch (RuntimeException e) {
            // the DB rollback can't remove the file from R2, so we delete it ourselves
            storageService.delete(storageKey);
            throw e;
        }

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

    @Transactional
    @Override
    public AttachmentResponseDTO uploadToMission(UUID missionId, MultipartFile file, String caption) {
        User currentUser = getLoggedUser();
        Tenant tenant = currentUser.getTenant();
        UUID currentTenantId = currentUser.getTenant().getId();
        Mission mission = missionRepo.findByIdAndCaseEntityTenantId(missionId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("mission not found !! "));

        if (!mission.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not assigned to this mission");
        }
        checkMissionAcceptsFiles(mission);
        if (file.isEmpty()) {
            throw new BadRequestException("the file is emtpty !!");
        }

        // the file also belongs to the case, so it shows in the case files and in the AI draft
        Case missionCase = mission.getCaseEntity();

        String originalName = file.getOriginalFilename();
        String uniqueName = UUID.randomUUID() + "-" + originalName;
        String storageKey =
                "tenants/" + currentTenantId +
                        "/cases/" + missionCase.getId() +
                        "/missions/" + missionId +
                        "/" + uniqueName;

        storageService.upload(file, storageKey);
        Attachment attachment = new Attachment();
        attachment.setCaseEntity(missionCase);
        attachment.setMission(mission);
        attachment.setStorageKey(storageKey);
        attachment.setUploadedBy(currentUser);
        attachment.setOriginalName(originalName);
        attachment.setTenant(tenant);
        attachment.setSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setCaption(caption);
        Attachment saved;
        try {
            // saveAndFlush so a DB error happens here and not later at commit
            saved = attachmentRepository.saveAndFlush(attachment);
        } catch (RuntimeException e) {
            // the DB rollback can't remove the file from R2, so we delete it ourselves
            storageService.delete(storageKey);
            throw e;
        }

        return attachmentMapper.toResponseDto(saved);
    }

    @Override
    public Page<AttachmentResponseDTO> getMissionAttachments(UUID missionId, Pageable pageable) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Mission mission = missionRepo.findByIdAndCaseEntityTenantId(missionId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("mission not found !! "));

        // Collaborateur can only see the files of his own mission
        if (currentUser.getRole() == Role.COLLABORATEUR
                && !mission.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not assigned to this mission");
        }

        Page<Attachment> missionAttachments = attachmentRepository.findAllByMissionIdAndTenantId(missionId, currentTenantId, pageable);

        return missionAttachments.map(attachmentMapper::toResponseDto);
    }

    // @Transactional so we can read attachment.getMission().getAssignedTo() (lazy)
    @Transactional(readOnly = true)
    @Override
    public AttachmentDownloadDTO downloadAttachment(UUID attachmentId) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, currentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found !! "));

        if (currentUser.getRole() == Role.COLLABORATEUR) {
            checkFileOfMyMission(attachment, currentUser);
        }

        byte[] content = storageService.download(attachment.getStorageKey());

        return new AttachmentDownloadDTO(
                content,
                attachment.getContentType(),
                attachment.getOriginalName()
        );
    }

    @Transactional
    @Override
    public void deleteAttachment(UUID attachmentId) {
        User currentUser = getLoggedUser();
        UUID currentTenantId = currentUser.getTenant().getId();
        Attachment currentAttachment = attachmentRepository.findByIdAndTenantId(attachmentId, currentTenantId).orElseThrow(
                () -> new ResourceNotFoundException("Attachment not found !! "));

        if (currentUser.getRole() == Role.COLLABORATEUR) {
            checkFileOfMyMission(currentAttachment, currentUser);
            checkMissionAcceptsFiles(currentAttachment.getMission());
        }

        storageService.delete(currentAttachment.getStorageKey());
        attachmentRepository.delete(currentAttachment);
    }

    // the collaborateur can add / delete files only while the mission is IN_PROGRESS or COMPLETED
    private void checkMissionAcceptsFiles(Mission mission) {
        if (mission.getStatus() != MissionStatus.IN_PROGRESS
                && mission.getStatus() != MissionStatus.COMPLETED) {
            throw new BadRequestException("Files can only be changed on an IN_PROGRESS or COMPLETED mission");
        }
    }

    // a collaborateur can only touch the files of his own missions (not the case files of the office)
    private void checkFileOfMyMission(Attachment attachment, User currentUser) {
        if (attachment.getMission() == null
                || !attachment.getMission().getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only access the files of your own missions");
        }
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

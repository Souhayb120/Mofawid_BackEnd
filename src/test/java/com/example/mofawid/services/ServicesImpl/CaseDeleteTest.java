package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.mapper.CaseMapper;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// tests of the case delete (CaseServiceImpl.deleteCase)
@ExtendWith(MockitoExtension.class)
class CaseDeleteTest {

    @Mock CaseRepo caseRepository;
    @Mock CaseMapper caseMapper;
    @Mock UserRepo userRepository;
    @Mock PartyRepo partyRepository;
    @Mock MissionRepo missionRepository;
    @Mock ObservationRepo observationRepository;
    @Mock DeliveryAttemptRepo deliveryAttemptRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock DocumentRepo documentRepository;
    @Mock BillingRepo billingRepository;
    @Mock CaseTrackingRepo caseTrackingRepository;
    @Mock StorageService storageService;

    @InjectMocks
    CaseServiceImpl caseService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final Case caseEntity = new Case();

    @BeforeEach
    void loginAsAdmin() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        User user = new User();
        user.setUsername("admin.demo");
        user.setTenant(tenant);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin.demo", null)
        );
        when(userRepository.findByUsername("admin.demo"))
                .thenReturn(Optional.of(user));
        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(caseEntity));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusedWhenTheCaseHasAnApprovedDocument() {
        when(documentRepository.existsByCaseEntityIdAndStatus(caseId, DocumentStatus.APPROVED))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> caseService.deleteCase(caseId));

        // nothing is deleted
        verify(caseRepository, never()).delete(any());
        verify(missionRepository, never()).deleteAll(any());
        verifyNoInteractions(storageService);
    }

    @Test
    void deletesTheChildrenBeforeTheCase() {
        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        List<Mission> missions = List.of(mission);

        Attachment attachment = new Attachment();
        attachment.setStorageKey("cases/photo-1.jpg");
        List<Attachment> attachments = List.of(attachment);

        when(missionRepository.findAllByCaseEntityId(caseId)).thenReturn(missions);
        when(attachmentRepository.findAllByCaseEntityIdOrderByUploadedAtAsc(caseId)).thenReturn(attachments);
        when(billingRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(caseTrackingRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());

        caseService.deleteCase(caseId);

        InOrder order = inOrder(attachmentRepository, observationRepository,
                deliveryAttemptRepository, missionRepository, partyRepository, caseRepository, storageService);
        order.verify(attachmentRepository).deleteAll(attachments);
        order.verify(observationRepository).deleteAll(any());
        order.verify(deliveryAttemptRepository).deleteAll(any());
        order.verify(missionRepository).deleteAll(missions);
        order.verify(partyRepository).deleteAll(any());
        order.verify(caseRepository).delete(caseEntity);
        order.verify(storageService).delete("cases/photo-1.jpg");
    }

    @Test
    void theCaseIsDeletedEvenIfAStorageFileFails() {
        Attachment attachment = new Attachment();
        attachment.setStorageKey("cases/photo-1.jpg");

        when(attachmentRepository.findAllByCaseEntityIdOrderByUploadedAtAsc(caseId)).thenReturn(List.of(attachment));
        when(missionRepository.findAllByCaseEntityId(caseId)).thenReturn(List.of());
        when(billingRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(caseTrackingRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("R2 down")).when(storageService).delete("cases/photo-1.jpg");

        assertDoesNotThrow(() -> caseService.deleteCase(caseId));

        verify(caseRepository).delete(caseEntity);
    }
}

package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.mapper.DocumentMapper;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.AiService;
import com.example.mofawid.services.ReportPdfService;
import com.example.mofawid.services.StorageService;
import com.example.mofawid.Prompt.DocumentPromptFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

// tests of the "Documents" page search (DocumentServiceImpl.searchDocuments)
@ExtendWith(MockitoExtension.class)
class DocumentSearchTest {

    @Mock MissionRepo missionRepository;
    @Mock ObservationRepo observationRepository;
    @Mock AiService aiService;
    @Mock DocumentRepo documentRepository;
    @Mock CaseRepo caseRepository;
    @Mock UserRepo userRepository;
    @Mock DocumentMapper documentMapper;
    @Mock DocumentPromptFactory documentPromptFactory;
    @Mock PartyRepo partyRepository;
    @Mock DeliveryAttemptRepo deliveryAttemptRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock ReportPdfService reportPdfService;
    @Mock StorageService storageService;

    @InjectMocks
    DocumentServiceImpl documentService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void loginAsAssistant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        User user = new User();
        user.setUsername("assistant.demo");
        user.setTenant(tenant);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("assistant.demo", null)
        );
        when(userRepository.findByUsername("assistant.demo"))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchUsesTheTenantOfTheCurrentUserAndCleansTheText() {
        when(documentRepository.searchDocuments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        documentService.searchDocuments(
                0, 10, "  Société ATLAS ", DocumentKind.NOTIFICATION, DocumentStatus.APPROVED,
                null, null, "createdAt", "desc"
        );

        verify(documentRepository).searchDocuments(
                eq(tenantId),
                eq(DocumentKind.NOTIFICATION),
                eq(DocumentStatus.APPROVED),
                eq("%société atlas%"),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void emptySearchMeansNoFilter() {
        when(documentRepository.searchDocuments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        documentService.searchDocuments(0, 10, "   ", null, null, null, null, null, null);

        verify(documentRepository).searchDocuments(
                eq(tenantId), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void theEndDateIsIncluded() {
        when(documentRepository.searchDocuments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        documentService.searchDocuments(
                0, 10, null, null, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null
        );

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(documentRepository).searchDocuments(
                eq(tenantId), isNull(), isNull(), isNull(), from.capture(), to.capture(), any(Pageable.class)
        );

        // Morocco is UTC+1: the 1st of October starts at 23:00 UTC the day before
        assertEquals(Instant.parse("2026-09-30T23:00:00Z"), from.getValue());
        // "to" is the start of the next day, so all of the 31st is included
        assertEquals(Instant.parse("2026-10-31T23:00:00Z"), to.getValue());
    }

    @Test
    void startDateAfterEndDateIsRefused() {
        assertThrows(BadRequestException.class, () ->
                documentService.searchDocuments(
                        0, 10, null, null, null,
                        LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1), null, null
                )
        );
        verifyNoInteractions(documentRepository);
    }

    @Test
    void sortByCaseRefUsesYearThenNumber() {
        when(documentRepository.searchDocuments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        documentService.searchDocuments(0, 10, null, null, null, null, null, "caseRef", "asc");

        Pageable pageable = capturePageable();
        assertEquals(
                Sort.by(Sort.Direction.ASC, "caseEntity.registerYear", "caseEntity.registerNumber"),
                pageable.getSort()
        );
    }

    @Test
    void unknownSortFieldFallsBackToCreatedAtAndPageSizeIsLimited() {
        when(documentRepository.searchDocuments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        documentService.searchDocuments(0, 5000, null, null, null, null, null, "content", "desc");

        Pageable pageable = capturePageable();
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
        assertEquals(100, pageable.getPageSize());
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository).searchDocuments(
                any(), any(), any(), any(), any(), any(), captor.capture()
        );
        return captor.getValue();
    }
}

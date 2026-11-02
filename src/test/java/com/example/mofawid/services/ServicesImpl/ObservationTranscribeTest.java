package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.ObservationDTO.TranscriptionResponseDTO;
import com.example.mofawid.exceptions.BadRequestException;
import com.example.mofawid.exceptions.ForbiddenException;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.mapper.ObservationMapper;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.AiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// tests of the voice -> text of the observations (ObservationServiceImpl.transcribeAudio)
@ExtendWith(MockitoExtension.class)
class ObservationTranscribeTest {

    @Mock ObservationRepo observationRepository;
    @Mock MissionRepo missionRepository;
    @Mock UserRepo userRepository;
    @Mock ObservationMapper observationMapper;
    @Mock AiService aiService;

    @InjectMocks
    ObservationServiceImpl observationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID missionId = UUID.randomUUID();
    private User collaborateur;

    private final MockMultipartFile audio = new MockMultipartFile(
            "audio", "observation.webm", "audio/webm;codecs=opus", new byte[]{1, 2, 3}
    );

    @BeforeEach
    void loginAsCollaborateur() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        collaborateur = new User();
        collaborateur.setId(UUID.randomUUID());
        collaborateur.setUsername("collab.demo");
        collaborateur.setTenant(tenant);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("collab.demo", null)
        );
        when(userRepository.findByUsername("collab.demo"))
                .thenReturn(Optional.of(collaborateur));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private void missionAssignedTo(User user) {
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setAssignedTo(user);
        when(missionRepository.findByIdAndCaseEntityTenantId(missionId, tenantId))
                .thenReturn(Optional.of(mission));
    }

    @Test
    void returnsTheTextOfTheRecording() throws Exception {
        missionAssignedTo(collaborateur);
        when(aiService.transcribe(any(), eq("audio/webm")))
                .thenReturn("  تمت معاينة المصعد وهو متوقف عن العمل.  \n");

        TranscriptionResponseDTO result = observationService.transcribeAudio(missionId, audio);

        // the codecs part is removed before sending to Gemini, the text is trimmed
        assertEquals("تمت معاينة المصعد وهو متوقف عن العمل.", result.text());
        verify(aiService).transcribe(audio.getBytes(), "audio/webm");
    }

    @Test
    void missionOfAnotherOfficeIsNotFound() {
        when(missionRepository.findByIdAndCaseEntityTenantId(missionId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> observationService.transcribeAudio(missionId, audio));
        verifyNoInteractions(aiService);
    }

    @Test
    void onlyTheAssignedCollaborateurCanTranscribe() {
        User other = new User();
        other.setId(UUID.randomUUID());
        missionAssignedTo(other);

        assertThrows(ForbiddenException.class,
                () -> observationService.transcribeAudio(missionId, audio));
        verifyNoInteractions(aiService);
    }

    @Test
    void emptyOrNonAudioFilesAreRefused() {
        missionAssignedTo(collaborateur);

        MockMultipartFile empty = new MockMultipartFile("audio", "a.webm", "audio/webm", new byte[0]);
        MockMultipartFile pdf = new MockMultipartFile("audio", "a.pdf", "application/pdf", new byte[]{1});

        assertThrows(BadRequestException.class,
                () -> observationService.transcribeAudio(missionId, empty));
        assertThrows(BadRequestException.class,
                () -> observationService.transcribeAudio(missionId, pdf));
        verifyNoInteractions(aiService);
    }

    @Test
    void aiErrorBecomesAClearMessage() {
        missionAssignedTo(collaborateur);
        when(aiService.transcribe(any(), any())).thenThrow(new RuntimeException("Gemini down"));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> observationService.transcribeAudio(missionId, audio));
        assertTrue(error.getMessage().contains("try again"));
    }

    @Test
    void silenceGivesAnEmptyText() {
        missionAssignedTo(collaborateur);
        when(aiService.transcribe(any(), any())).thenReturn(null);

        assertEquals("", observationService.transcribeAudio(missionId, audio).text());
    }
}

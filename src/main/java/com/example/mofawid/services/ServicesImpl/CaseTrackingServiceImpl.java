package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.TrackingDTO.ClientTrackingResponseDTO;
import com.example.mofawid.DTO.TrackingDTO.TrackingCodeResponseDTO;
import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.models.Billing;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.CaseTracking;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.BillingRepo;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.CaseTrackingRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.CaseTrackingService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CaseTrackingServiceImpl
        implements CaseTrackingService {

    private final CaseTrackingRepo caseTrackingRepository;
    private final CaseRepo caseRepository;
    private final BillingRepo billingRepository;
    private final UserRepo userRepository;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Value("${tracking.secret}")
    private String trackingSecret;

    public CaseTrackingServiceImpl(
            CaseTrackingRepo caseTrackingRepository,
            CaseRepo caseRepository,
            BillingRepo billingRepository,
            UserRepo userRepository
    ) {
        this.caseTrackingRepository = caseTrackingRepository;
        this.caseRepository = caseRepository;
        this.billingRepository = billingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public TrackingCodeResponseDTO generateTrackingCode(
            UUID caseId
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Make sure the Case belongs to current tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Case not found"
                        )
                );

        // Only one tracking code per Case
        if (caseTrackingRepository
                .findByCaseEntityId(caseId)
                .isPresent()) {

            throw new BadRequestException(
                    "Tracking code already exists for this case"
            );
        }

        // Generate code
        String trackingCode =
                generateCode();

        // Store only hash
        String codeHash =
                hashCode(trackingCode);

        CaseTracking caseTracking =
                new CaseTracking();

        caseTracking.setCaseEntity(
                caseEntity
        );

        caseTracking.setCodeHash(
                codeHash
        );

        caseTrackingRepository.save(
                caseTracking
        );

        // Return real code only now
        return new TrackingCodeResponseDTO(
                caseEntity.getRef(),
                trackingCode
        );
    }

    @Override
    public ClientTrackingResponseDTO trackCase(
            String trackingCode
    ) {

        String normalizedCode =
                normalizeCode(trackingCode);

        String codeHash =
                hashCode(normalizedCode);

        CaseTracking caseTracking =
                caseTrackingRepository
                        .findByCodeHash(codeHash)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Invalid tracking code"
                                )
                        );

        Case caseEntity =
                caseTracking.getCaseEntity();

        Billing billing =
                billingRepository
                        .findByCaseEntityId(
                                caseEntity.getId()
                        )
                        .orElse(null);

        ClientTrackingResponseDTO response =
                new ClientTrackingResponseDTO();

        response.setCaseRef(
                caseEntity.getRef()
        );

        response.setProgress(
                getPublicStatus(
                        caseEntity.getCaseStatus()
                )
        );

        response.setReportReady(
                caseEntity.getCaseStatus()
                        == CaseStatus.CLOSED
        );

        // Billing may not exist yet
        if (billing != null) {

            response.setTotalAmount(
                    billing.getTotalAmount()
            );

            response.setAmountPaid(
                    billing.getAmountPaid()
            );

            response.setRemainingAmount(
                    billing.getRemainingAmount()
            );

            response.setPaymentStatus(
                    billing.getStatus()
            );

        } else {

            response.setTotalAmount(
                    BigDecimal.ZERO
            );

            response.setAmountPaid(
                    BigDecimal.ZERO
            );

            response.setRemainingAmount(
                    BigDecimal.ZERO
            );

            response.setPaymentStatus(null);
        }

        return response;
    }

    private String generateCode() {

        String characters =
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        StringBuilder code =
                new StringBuilder("MF-");

        for (int i = 0; i < 12; i++) {

            if (i > 0 && i % 4 == 0) {
                code.append("-");
            }

            int index =
                    secureRandom.nextInt(
                            characters.length()
                    );

            code.append(
                    characters.charAt(index)
            );
        }

        return code.toString();
    }

    private String normalizeCode(
            String code
    ) {

        return code
                .trim()
                .toUpperCase();
    }

    private String hashCode(
            String code
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            trackingSecret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    );

            mac.init(secretKey);

            byte[] hash =
                    mac.doFinal(
                            code.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not hash tracking code",
                    e
            );
        }
    }

    private String getPublicStatus(
            CaseStatus status
    ) {

        return switch (status) {

            case QUALIFIED ->
                    "Dossier reçu";

            case ASSIGNED ->
                    "Dossier pris en charge";

            case IN_PROGRESS ->
                    "Traitement en cours";

            case AWAITING_APPROVAL ->
                    "Rapport en préparation";

            case CLOSED ->
                    "Rapport prêt";
        };
    }

    private User getCurrentUser() {

        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current user not found"
                        )
                );
    }
}
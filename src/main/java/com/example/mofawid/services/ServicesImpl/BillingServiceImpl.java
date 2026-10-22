package com.example.mofawid.services.ServicesImpl;
import com.example.mofawid.DTO.BillingDTO.BillingRequestDTO;
import com.example.mofawid.DTO.BillingDTO.BillingResponseDTO;

import com.example.mofawid.Enums.PaymentStatus;
import com.example.mofawid.mapper.BillingMapper;
import com.example.mofawid.models.Billing;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.BillingRepo;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.BillingService;
import com.example.mofawid.exceptions.ResourceNotFoundException;
import com.example.mofawid.exceptions.BadRequestException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
@Service
public class BillingServiceImpl implements BillingService {

    private final BillingRepo billingRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final BillingMapper billingMapper;

    public BillingServiceImpl(
            BillingRepo billingRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            BillingMapper billingMapper
    ) {
        this.billingRepository = billingRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.billingMapper = billingMapper;
    }

    @Transactional
    @Override
    public BillingResponseDTO createBilling(
            UUID caseId,
            BillingRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Case caseEntity = caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        if (billingRepository
                .findByCaseEntityId(caseId)
                .isPresent()) {

            throw new BadRequestException(
                    "Billing already exists for this case"
            );
        }

        Billing billing =
                billingMapper.toEntity(dto);

        billing.setCaseEntity(caseEntity);

        calculateBilling(billing);

        Billing savedBilling =
                billingRepository.save(billing);

        return billingMapper.toResponseDto(
                savedBilling
        );
    }

    @Override
    public BillingResponseDTO getBillingByCase(
            UUID caseId
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // First protect tenant isolation
        caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        Billing billing =
                billingRepository
                        .findByCaseEntityId(caseId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Billing not found"
                                )
                        );

        return billingMapper.toResponseDto(
                billing
        );
    }

    @Transactional
    @Override
    public BillingResponseDTO updateBilling(
            UUID caseId,
            BillingRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Case not found")
                );

        Billing billing =
                billingRepository
                        .findByCaseEntityId(caseId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Billing not found"
                                )
                        );

        billing.setTotalAmount(
                dto.getTotalAmount()
        );

        billing.setAmountPaid(
                dto.getAmountPaid()
        );

        billing.setDescription(
                dto.getDescription()
        );

        calculateBilling(billing);

        Billing updatedBilling =
                billingRepository.save(billing);

        return billingMapper.toResponseDto(
                updatedBilling
        );
    }

    private void calculateBilling(Billing billing) {

        BigDecimal total =
                billing.getTotalAmount();

        BigDecimal paid =
                billing.getAmountPaid();

        if (paid.compareTo(total) > 0) {
            throw new BadRequestException(
                    "Amount paid cannot be greater than total amount"
            );
        }

        BigDecimal remaining =
                total.subtract(paid);

        billing.setRemainingAmount(
                remaining
        );

        if (paid.compareTo(BigDecimal.ZERO) == 0) {

            billing.setStatus(
                    PaymentStatus.UNPAID
            );

        } else if (paid.compareTo(total) < 0) {

            billing.setStatus(
                    PaymentStatus.PARTIALLY_PAID
            );

        } else {

            billing.setStatus(
                    PaymentStatus.PAID
            );
        }
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

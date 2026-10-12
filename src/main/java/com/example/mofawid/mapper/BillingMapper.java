package com.example.mofawid.mapper;
import com.example.mofawid.DTO.BillingDTO.BillingRequestDTO;
import com.example.mofawid.DTO.BillingDTO.BillingResponseDTO;
import com.example.mofawid.models.Billing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface BillingMapper {

    @Mapping(target = "caseEntity", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    Billing toEntity(BillingRequestDTO dto);

    @Mapping(source = "caseEntity.id", target = "caseId")
    @Mapping(source = "caseEntity.ref", target = "caseRef")
    BillingResponseDTO toResponseDto(Billing billing);
}
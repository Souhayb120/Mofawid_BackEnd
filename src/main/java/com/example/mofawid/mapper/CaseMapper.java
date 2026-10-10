package com.example.mofawid.mapper;

import com.example.mofawid.DTO.CaseDTO.CaseRequestDTO;
import com.example.mofawid.DTO.CaseDTO.CaseResponseDTO;
import com.example.mofawid.models.Case;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CaseMapper {

    Case toEntity(CaseRequestDTO dto);

    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    CaseResponseDTO toResponseDto(Case caseEntity);
}
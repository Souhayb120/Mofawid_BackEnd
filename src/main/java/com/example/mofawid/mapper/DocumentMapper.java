package com.example.mofawid.mapper;

import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.models.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "caseEntity", ignore = true)
    @Mapping(target = "status", ignore = true)
    Document toEntity(DocumentRequestDTO dto);

    @Mapping(source = "caseEntity.id", target = "caseId")
    @Mapping(source = "caseEntity.ref", target = "caseRef")
    @Mapping(source = "approvedBy.username", target = "approvedByUsername")
    DocumentResponseDTO toResponseDto(Document document);
}
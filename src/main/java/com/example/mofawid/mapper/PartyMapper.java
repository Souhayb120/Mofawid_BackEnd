package com.example.mofawid.mapper;

import com.example.mofawid.DTO.PartyDTO.PartyRequestDTO;
import com.example.mofawid.DTO.PartyDTO.PartyResponseDTO;
import com.example.mofawid.models.Party;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PartyMapper {

    @Mapping(target = "caseEntity", ignore = true)
    Party toEntity(PartyRequestDTO dto);

    @Mapping(source = "caseEntity.id", target = "caseId")
    PartyResponseDTO toResponseDto(Party party);

    @Mapping(target = "caseEntity", ignore = true)
    void updateEntityFromDto(
            PartyRequestDTO dto,
            @MappingTarget Party party
    );
}

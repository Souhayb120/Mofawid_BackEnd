package com.example.mofawid.mapper;

import com.example.mofawid.DTO.MissionDTO.MissionRequestDTO;
import com.example.mofawid.DTO.MissionDTO.MissionResponseDTO;
import com.example.mofawid.models.Mission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MissionMapper {

    @Mapping(target = "caseEntity", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    Mission toEntity(MissionRequestDTO dto);

    @Mapping(source = "caseEntity.id", target = "caseId")
    @Mapping(source = "caseEntity.ref", target = "caseRef")

    @Mapping(source = "assignedTo.id", target = "assignedToId")
    @Mapping(source = "assignedTo.username", target = "assignedToUsername")

    @Mapping(source = "assignedBy.id", target = "assignedById")
    @Mapping(source = "assignedBy.username", target = "assignedByUsername")

    MissionResponseDTO toResponseDto(Mission mission);
}
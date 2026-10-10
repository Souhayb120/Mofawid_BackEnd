package com.example.mofawid.mapper;

import com.example.mofawid.DTO.ObservationDTO.ObservationRequestDTO;
import com.example.mofawid.DTO.ObservationDTO.ObservationResponseDTO;
import com.example.mofawid.models.Observation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ObservationMapper {

    @Mapping(target = "mission", ignore = true)
    Observation toEntity(ObservationRequestDTO dto);

    @Mapping(source = "mission.id", target = "missionId")
    ObservationResponseDTO toResponseDto(Observation observation);
}
package com.example.mofawid.mapper;

import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptRequestDTO;
import com.example.mofawid.DTO.DeliveryAttemptDTO.DeliveryAttemptResponseDTO;
import com.example.mofawid.models.DeliveryAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeliveryAttemptMapper {

    @Mapping(target = "mission", ignore = true)
    DeliveryAttempt toEntity(DeliveryAttemptRequestDTO dto);

    @Mapping(source = "mission.id", target = "missionId")
    DeliveryAttemptResponseDTO toResponseDto(DeliveryAttempt attempt);
}

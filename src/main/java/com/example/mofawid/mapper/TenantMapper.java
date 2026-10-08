package com.example.mofawid.mapper;

import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.models.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantMapper {
    Tenant toEntity(TenantRequestDTO dto);

    TenantResposeDTO toResponseDto(Tenant tenant);
    void updateEntityFromDto(
            TenantRequestDTO dto,
            @MappingTarget Tenant tenant
    );
}

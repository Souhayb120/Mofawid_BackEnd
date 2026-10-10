package com.example.mofawid.services;

import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;


public interface TenantService {
    TenantResposeDTO createTenant(TenantRequestDTO tenantRequestDTO);


    TenantResposeDTO getTenantById(UUID id);

    Page<TenantResposeDTO> getAllTenants(int page , int size);

    TenantResposeDTO updateTenant(UUID id, TenantRequestDTO dto);

    void deleteTenant(UUID id);

    UserResposeDTO createTenantAdmin(UUID tenantId, UserRegisterDTO dto);

}

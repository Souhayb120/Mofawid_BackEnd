package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.mapper.TenantMapper;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.repositories.TenantRepo;
import com.example.mofawid.services.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepo tenantRepository;
    private final TenantMapper tenantMapper;

    public TenantServiceImpl(
            TenantRepo tenantRepository,
            TenantMapper tenantMapper
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public TenantResposeDTO createTenant(TenantRequestDTO dto) {

        Tenant tenant = tenantMapper.toEntity(dto);

        Tenant savedTenant = tenantRepository.save(tenant);

        return tenantMapper.toResponseDto(savedTenant);
    }

    @Override
    public TenantResposeDTO getTenantById(UUID id) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found"));

        return tenantMapper.toResponseDto(tenant);
    }

    @Override
    public Page<TenantResposeDTO> getAllTenants(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Tenant> tenants =
                tenantRepository.findAll(pageable);

        return tenants.map(tenantMapper::toResponseDto);
    }

    @Override
    public TenantResposeDTO updateTenant(
            UUID id,
            TenantRequestDTO dto
    ) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found"));

        tenantMapper.updateEntityFromDto(dto, tenant);

        Tenant updatedTenant =
                tenantRepository.save(tenant);

        return tenantMapper.toResponseDto(updatedTenant);
    }

    @Override
    public void deleteTenant(UUID id) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found"));

        tenantRepository.delete(tenant);
    }
}
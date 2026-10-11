package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.Enums.Role;
import com.example.mofawid.mapper.TenantMapper;
import com.example.mofawid.mapper.UserMapper;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.CaseRepo;
import com.example.mofawid.repositories.TenantRepo;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantServiceImpl implements TenantService {
private final CaseRepo caseRepository;
    private final TenantRepo tenantRepository;
    private final TenantMapper tenantMapper;
    private final UserRepo userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public TenantServiceImpl(
            CaseRepo caseRepository, TenantRepo tenantRepository,
            TenantMapper tenantMapper,
            UserRepo userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.caseRepository = caseRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public TenantResposeDTO createTenant(TenantRequestDTO dto) {

        Tenant tenant = tenantMapper.toEntity(dto);

        Tenant savedTenant =
                tenantRepository.save(tenant);

        return tenantMapper.toResponseDto(savedTenant);
    }

    @Override
    public TenantResposeDTO getTenantById(UUID id) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found")
                );

        return tenantMapper.toResponseDto(tenant);
    }

    @Override
    public Page<TenantResposeDTO> getAllTenants(
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Tenant> tenants =
                tenantRepository.findAll(pageable);

        return tenants.map(
                tenantMapper::toResponseDto
        );
    }

    @Override
    public TenantResposeDTO updateTenant(
            UUID id,
            TenantRequestDTO dto
    ) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found")
                );

        tenantMapper.updateEntityFromDto(
                dto,
                tenant
        );

        Tenant updatedTenant =
                tenantRepository.save(tenant);

        return tenantMapper.toResponseDto(
                updatedTenant
        );
    }


    @Override
    public void deleteTenant(UUID id) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found")
                );

        if (userRepository.existsByTenantId(id)) {
            throw new RuntimeException(
                    "Cannot delete tenant because it still has users"
            );
        }

        if (caseRepository.existsByTenantId(id)) {
            throw new RuntimeException(
                    "Cannot delete tenant because it still has cases"
            );
        }

        tenantRepository.delete(tenant);
    }

    @Override
    public UserResposeDTO createTenantAdmin(
            UUID tenantId,
            UserRegisterDTO dto
    ) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found")
                );

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException(
                    "Username already exists"
            );
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }


        User admin = userMapper.toEntity(dto);


        admin.setTenant(tenant);


        admin.setRole(Role.ADMIN);


        admin.setPassword(
                passwordEncoder.encode(
                        dto.getPassword()
                )
        );

        admin.setActive(true);


        User savedAdmin =
                userRepository.save(admin);


        return userMapper.toResponseDto(
                savedAdmin
        );
    }
}
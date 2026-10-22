package com.example.mofawid.controllers;


import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.services.TenantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResposeDTO> createTenant(
            @Valid @RequestBody TenantRequestDTO dto
    ) {
        return ResponseEntity.ok(
                tenantService.createTenant(dto)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResposeDTO> getTenantById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                tenantService.getTenantById(id)
        );
    }

    // the office of the logged user (used for the letterhead of the reports)
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASSISTANT')")
    public ResponseEntity<TenantResposeDTO> getMyTenant() {
        return ResponseEntity.ok(
                tenantService.getMyTenant()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<TenantResposeDTO>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                tenantService.getAllTenants(page, size)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResposeDTO> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequestDTO dto
    ) {
        return ResponseEntity.ok(
                tenantService.updateTenant(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTenant(
            @PathVariable UUID id
    ) {
        tenantService.deleteTenant(id);

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{tenantId}/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResposeDTO> createTenantAdmin(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UserRegisterDTO dto
    ) {
        return ResponseEntity.ok(
                tenantService.createTenantAdmin(tenantId, dto)
        );
    }
}
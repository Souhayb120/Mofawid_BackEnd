package com.example.mofawid.controllers;


import com.example.mofawid.DTO.TenantDTO.TenantRequestDTO;
import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.services.TenantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TenantResposeDTO> createTenant(
            @Valid @RequestBody TenantRequestDTO dto
    ) {
        return ResponseEntity.ok(
                tenantService.createTenant(dto)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResposeDTO> getTenantById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                tenantService.getTenantById(id)
        );
    }

    @GetMapping
    public ResponseEntity<Page<TenantResposeDTO>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                tenantService.getAllTenants(page, size)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenantResposeDTO> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequestDTO dto
    ) {
        return ResponseEntity.ok(
                tenantService.updateTenant(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(
            @PathVariable UUID id
    ) {
        tenantService.deleteTenant(id);

        return ResponseEntity.noContent().build();
    }
}
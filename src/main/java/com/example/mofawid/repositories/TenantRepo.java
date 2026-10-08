package com.example.mofawid.repositories;

import com.example.mofawid.DTO.TenantDTO.TenantResposeDTO;
import com.example.mofawid.models.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepo extends JpaRepository<Tenant, UUID> {
}

package com.example.mofawid.repositories;

import com.example.mofawid.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {

    Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

}
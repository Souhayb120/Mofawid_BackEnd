package com.example.mofawid.DTO.UserDTO;

import com.example.mofawid.Enums.Role;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserResposeDTO {
    private UUID id;

    private String username;

    private String email;

    private String nom;

    private String prenom;

    private String phone;

    private Boolean active;

    private Role role;

    private String licenceNumber;

    private Instant createdAt;
}

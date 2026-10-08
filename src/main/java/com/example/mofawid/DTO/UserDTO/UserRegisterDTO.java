package com.example.mofawid.DTO.UserDTO;

import com.example.mofawid.Enums.Role;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;
@Data
public class UserRegisterDTO {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String nom;

    private String prenom;

    private String phone;

    @NotNull
    private Role role;

    // Used only when role = ADMIN
    private String licenceNumber;
}

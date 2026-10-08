package com.example.mofawid.DTO.UserDTO;

import com.example.mofawid.Enums.Role;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateDTO {
    private String username;

    @Email
    private String email;

    private String nom;

    private String prenom;

    private String phone;

    private Role role;

    private Boolean active;
}

package com.example.mofawid.DTO.UserDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 6)
    private String password;
}

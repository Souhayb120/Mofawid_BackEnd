package com.example.mofawid.DTO.UserDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotBlank
    private String username;
    @NotBlank
    @Min(8)
    private String password;
}

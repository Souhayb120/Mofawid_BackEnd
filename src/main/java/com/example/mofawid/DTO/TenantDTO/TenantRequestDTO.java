package com.example.mofawid.DTO.TenantDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRequestDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String status;

    private String adress;

    private String phone;
}

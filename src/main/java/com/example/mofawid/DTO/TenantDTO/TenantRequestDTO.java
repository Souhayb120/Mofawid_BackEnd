package com.example.mofawid.DTO.TenantDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRequestDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    @NotBlank
    private String status;

    @NotBlank
    private String adress;

    @NotBlank
    private String phone;

    private String huissierNameAr;

    private String huissierNameFr;

    private String appealCourtAr;

    private String appealCourtFr;

    private String firstInstanceCourtAr;

    private String addressFr;

    private String email;

    private String city;

    private Integer startNumber;
}

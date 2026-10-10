package com.example.mofawid.DTO.TenantDTO;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TenantResposeDTO {
    private UUID id;

    private String name;

    private String slug;

    private String status;

    private String adress;

    private String phone;

    private Instant createdAt;
}

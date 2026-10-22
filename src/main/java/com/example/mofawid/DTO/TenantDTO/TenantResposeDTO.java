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

    private String huissierNameAr;

    private String huissierNameFr;

    private String appealCourtAr;

    private String appealCourtFr;

    private String firstInstanceCourtAr;

    private String addressFr;

    private String email;

    private String city;

    private Integer startNumber;

    private Instant createdAt;
}

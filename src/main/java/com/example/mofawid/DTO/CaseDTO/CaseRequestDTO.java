package com.example.mofawid.DTO.CaseDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CaseRequestDTO {

    @NotBlank
    private String clientName;

    private String clientIce;

    @NotBlank
    private String address;

    private String phone;

    @NotBlank
    private String subject;

    private String description;

    @NotNull
    private LocalDate deadline;
}
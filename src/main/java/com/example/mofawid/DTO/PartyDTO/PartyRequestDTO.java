package com.example.mofawid.DTO.PartyDTO;

import com.example.mofawid.Enums.PartyRole;
import com.example.mofawid.Enums.PartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartyRequestDTO {

    @NotNull
    private PartyRole role;

    @NotNull
    private PartyType type;

    @NotBlank
    private String fullName;

    private String cin;

    private String rc;

    private String legalRepresentative;

    private String address;

    private String phone;

    private String capacity;
}

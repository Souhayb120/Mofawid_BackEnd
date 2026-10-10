package com.example.mofawid.DTO.ObservationDTO;

import com.example.mofawid.Enums.ObservationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ObservationRequestDTO {

    @NotNull
    private ObservationKind kind;

    @NotBlank
    private String description;
}
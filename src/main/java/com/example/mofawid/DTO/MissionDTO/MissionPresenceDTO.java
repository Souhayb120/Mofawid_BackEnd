package com.example.mofawid.DTO.MissionDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// the person present during the constat
@Data
public class MissionPresenceDTO {

    @NotBlank
    private String presentPersonName;

    @NotBlank
    private String presentPersonCapacity;
}

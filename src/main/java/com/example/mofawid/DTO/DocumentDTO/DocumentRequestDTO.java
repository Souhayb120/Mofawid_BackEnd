package com.example.mofawid.DTO.DocumentDTO;

import com.example.mofawid.Enums.DocumentKind;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentRequestDTO {

    @NotNull
    private DocumentKind kind;

    private String content;
}
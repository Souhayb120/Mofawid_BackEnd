package com.example.mofawid.DTO.AttachmentDTO;



public record AttachmentDownloadDTO(
        byte[] content,
        String contentType,
        String originalName
) {
}

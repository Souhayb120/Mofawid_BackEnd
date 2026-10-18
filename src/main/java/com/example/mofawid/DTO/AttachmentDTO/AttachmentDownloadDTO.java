package com.example.mofawid.DTO.AttachmentDTO;

/**
 * Contenu binaire d'une pièce jointe + métadonnées nécessaires à la réponse HTTP.
 * Ne jamais exposer la clé de stockage R2 côté client.
 */
public record AttachmentDownloadDTO(
        byte[] content,
        String contentType,
        String originalName
) {
}

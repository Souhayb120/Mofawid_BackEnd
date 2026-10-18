package com.example.mofawid.mapper;



import com.example.mofawid.DTO.AttachmentDTO.AttachmentResponseDTO;
import com.example.mofawid.models.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(source = "caseEntity.id", target = "caseId")
    @Mapping(source = "uploadedBy.id", target = "uploadedById")
    AttachmentResponseDTO toResponseDto(Attachment attachment);
}
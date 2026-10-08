package com.example.mofawid.mapper;

import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.models.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRegisterDTO dto);

    UserResposeDTO toResponseDto(User user);
}

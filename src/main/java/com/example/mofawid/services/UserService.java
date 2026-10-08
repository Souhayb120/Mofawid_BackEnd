package com.example.mofawid.services;

import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserUpdateDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {

    UserResposeDTO createUser(UserRegisterDTO dto);

    UserResposeDTO getUserById(UUID id);

    Page<UserResposeDTO> getAllUsers(int page, int size);

    UserResposeDTO updateUser(UUID id, UserUpdateDTO dto);

    void deleteUser(UUID id);
}
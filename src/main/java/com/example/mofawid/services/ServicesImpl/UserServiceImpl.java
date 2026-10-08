package com.example.mofawid.services.ServicesImpl;


import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserUpdateDTO;
import com.example.mofawid.mapper.UserMapper;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.UserRepo;
import com.example.mofawid.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepo userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepo userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResposeDTO createUser(UserRegisterDTO dto) {

        User user = userMapper.toEntity(dto);
        User currentUser = getCurrentUser();
        user.setTenant(currentUser.getTenant());
        user.setPassword(
                passwordEncoder.encode(dto.getPassword())
        );

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDto(savedUser);

    }

    @Override
    public UserResposeDTO getUserById(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        User user = userRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return userMapper.toResponseDto(user);
    }

    @Override
    public Page<UserResposeDTO> getAllUsers(int page, int size) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Pageable pageable =
                PageRequest.of(page, size);

        Page<User> users =
                userRepository.findAllByTenantId(
                        tenantId,
                        pageable
                );

        return users.map(userMapper::toResponseDto);
    }

    @Override
    public UserResposeDTO updateUser(
            UUID id,
            UserUpdateDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        User user = userRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setNom(dto.getNom());
        user.setPrenom(dto.getPrenom());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setActive(dto.getActive());

        User updatedUser =
                userRepository.save(user);

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        User user = userRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        userRepository.delete(user);
    }


    private User getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Current user not found"));
    }
}
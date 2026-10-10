package com.example.mofawid.controllers;

import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.DTO.UserDTO.UserResposeDTO;
import com.example.mofawid.DTO.UserDTO.UserUpdateDTO;
import com.example.mofawid.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResposeDTO> createUser(
            @Valid @RequestBody UserRegisterDTO dto
    ) {
        return ResponseEntity.ok(
                userService.createUser(dto)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResposeDTO> getUserById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResposeDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                userService.getAllUsers(page, size)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResposeDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateDTO dto
    ) {
        return ResponseEntity.ok(
                userService.updateUser(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(
            @PathVariable UUID id
    ) {
        userService.deleteUser(id);


    }
}
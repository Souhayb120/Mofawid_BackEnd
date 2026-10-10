package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.UserDTO.AuthResponse;
import com.example.mofawid.DTO.UserDTO.UserLoginDTO;
import com.example.mofawid.DTO.UserDTO.UserRegisterDTO;
import com.example.mofawid.config.JwtUtil;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepo userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(UserRegisterDTO request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setPhone(request.getPhone());

        user.setRole(request.getRole());

        user.setActive(true);

        userRepository.save(user);
    }

    public AuthResponse login(UserLoginDTO dto) {

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(
                dto.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(token);
    }
}

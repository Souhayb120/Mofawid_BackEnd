package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.DTO.UserDTO.AuthResponse;
import com.example.mofawid.DTO.UserDTO.UserLoginDTO;
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

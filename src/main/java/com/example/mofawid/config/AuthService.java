package com.example.mofawid.config;

import com.example.kottabi.DTO.UserDTO.AuthResponse;
import com.example.kottabi.DTO.UserDTO.UserAuthRequest;
import com.example.kottabi.DTO.UserDTO.UserLoginDTO;
import com.example.kottabi.enums.Role;
import com.example.kottabi.models.Admin;
import com.example.kottabi.models.Eleve;
import com.example.kottabi.models.Enseignant;
import com.example.kottabi.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {


	private final AuthenticationManager authenticationManager;

	private final JwtUtil jwtUtil;


	private final UserService userDetailsService;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userDetailsService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(UserAuthRequest request) {
		Eleve eleve = new Eleve();
		Enseignant enseignant = new Enseignant();
		Admin admin = new Admin();
		switch (request.getRole().name()) {
			case "ELEVE" -> {
				eleve.setUsername(request.getUsername());
				eleve.setRole(Role.ELEVE);
				eleve.setPassword(passwordEncoder.encode(request.getPassword()));
				eleve.setPrenom(request.getPrenom());
				eleve.setNom(request.getNom());
				eleve.setDateNaissance(request.getDateNaissance());
				eleve.setTel(request.getTel());
				userRepository.save(eleve);
			}
			case "ADMIN" -> {
				admin.setUsername(request.getUsername());
				admin.setTel(request.getTel());
				admin.setPassword(passwordEncoder.encode(request.getPassword()));
				admin.setRole(Role.ADMIN);
				userRepository.save(admin);
			}
			case "ENSEIGNANT" -> {
				enseignant.setUsername(request.getUsername());
				enseignant.setNom(request.getNom());
				eleve.setPrenom(request.getPrenom());
				enseignant.setTel(request.getTel());
				enseignant.setPassword(passwordEncoder.encode(request.getPassword()));
				enseignant.setRole(Role.ENSEIGNANT);
				enseignant.setSpecialite(request.getSpecialite());
				enseignant.setDescription(request.getDescription());
				userRepository.save(enseignant);
			}
		}
	}

	public AuthResponse login(UserLoginDTO request) {
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);

		UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

		String token = jwtUtil.generateToken(user);
		return new AuthResponse(token);
	}
}

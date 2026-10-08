package com.example.mofawid.config;

import com.example.kottabi.Exceptions.ResourceNotFoundException;
import com.example.kottabi.models.UserEntity;
import com.example.kottabi.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository
			.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	public Page<UserEntity> findAllUsers(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		return userRepository.findAll(pageable);
	}

	public UserEntity findUserById(long id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found !!"));
	}

	public void supprimerUser(long id) {
		UserEntity user = userRepository
			.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("User not found !!"));

		userRepository.delete(user);
	}
}

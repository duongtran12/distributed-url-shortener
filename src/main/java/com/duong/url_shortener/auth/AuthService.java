package com.duong.url_shortener.auth;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.EmailNormalizer;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String normalizedEmail = EmailNormalizer.normalize(request.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw emailAlreadyExists();
		}

		User user = User.create(
				normalizedEmail,
				passwordEncoder.encode(request.password()),
				request.displayName());

		try {
			return RegisterResponse.from(userRepository.saveAndFlush(user));
		} catch (DataIntegrityViolationException exception) {
			throw emailAlreadyExists();
		}
	}

	private ApiException emailAlreadyExists() {
		return new ApiException(
				HttpStatus.CONFLICT,
				"EMAIL_ALREADY_EXISTS",
				"An account with this email already exists");
	}
}

package com.duong.url_shortener.auth;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.security.JwtTokenService;
import com.duong.url_shortener.user.EmailNormalizer;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = EmailNormalizer.normalize(request.email());

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
		} catch (AuthenticationException exception) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
		}

		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_CREDENTIALS",
						"Invalid email or password"));

		return new LoginResponse(
				jwtTokenService.createAccessToken(user),
				"Bearer",
				jwtTokenService.accessTokenExpiresInSeconds());
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

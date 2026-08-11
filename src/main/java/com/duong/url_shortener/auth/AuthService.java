package com.duong.url_shortener.auth;

import com.duong.url_shortener.common.exception.ApiException;
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
	private final RefreshTokenService refreshTokenService;
	private final EmailVerificationService emailVerificationService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			RefreshTokenService refreshTokenService,
			EmailVerificationService emailVerificationService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.refreshTokenService = refreshTokenService;
		this.emailVerificationService = emailVerificationService;
	}

	@Transactional
	public AuthSession login(LoginRequest request, String userAgent) {
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

		return refreshTokenService.create(user, userAgent);
	}

	public AuthSession refresh(String refreshToken, String userAgent) {
		return refreshTokenService.rotate(refreshToken, userAgent);
	}

	public void logout(String refreshToken) {
		refreshTokenService.revoke(refreshToken);
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String normalizedEmail = EmailNormalizer.normalize(request.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw emailAlreadyExists();
		}

		User user = User.createPendingVerification(
				normalizedEmail,
				passwordEncoder.encode(request.password()),
				request.displayName());

		try {
			User saved = userRepository.saveAndFlush(user);
			emailVerificationService.sendVerification(saved);
			return RegisterResponse.from(saved);
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

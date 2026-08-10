package com.duong.url_shortener.user;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.auth.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final JdbcTemplate jdbcTemplate;

	public UserService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService,
			JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getCurrentUser(Long userId) {
		return UserProfileResponse.from(findActiveUser(userId));
	}

	@Transactional
	public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = findActiveUser(userId);
		user.updateDisplayName(request.displayName());
		return UserProfileResponse.from(user);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = findActiveUser(userId);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"INVALID_CURRENT_PASSWORD",
					"The current password is incorrect");
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"PASSWORD_UNCHANGED",
					"The new password must be different from the current password");
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenService.revokeAllForUser(userId);
	}

	@Transactional
	public void deleteAccount(Long userId, DeleteAccountRequest request) {
		User user = findActiveUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"INVALID_CURRENT_PASSWORD",
					"The current password is incorrect");
		}

		jdbcTemplate.update("""
				DELETE FROM click_events event
				USING short_urls short_url
				WHERE event.short_code = short_url.short_code
				  AND short_url.user_id = ?
				""", userId);
		userRepository.delete(user);
		userRepository.flush();
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_ACCESS_TOKEN",
						"The access token no longer belongs to an existing user"));

		if (!user.isEnabled()) {
			throw new ApiException(
					HttpStatus.FORBIDDEN,
					"ACCOUNT_DISABLED",
					"The user account is disabled");
		}

		return user;
	}
}

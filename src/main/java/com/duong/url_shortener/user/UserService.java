package com.duong.url_shortener.user;

import com.duong.url_shortener.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getCurrentUser(Long userId) {
		return UserProfileResponse.from(findActiveUser(userId));
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

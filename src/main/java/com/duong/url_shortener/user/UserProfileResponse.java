package com.duong.url_shortener.user;

import java.time.Instant;

public record UserProfileResponse(
		Long id,
		String email,
		String displayName,
		Role role,
		Instant createdAt) {

	static UserProfileResponse from(User user) {
		return new UserProfileResponse(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				user.getCreatedAt());
	}
}

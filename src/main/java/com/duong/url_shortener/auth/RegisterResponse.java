package com.duong.url_shortener.auth;

import java.time.Instant;

import com.duong.url_shortener.user.Role;
import com.duong.url_shortener.user.User;

public record RegisterResponse(
		Long id,
		String email,
		String displayName,
		Role role,
		Instant createdAt) {

	static RegisterResponse from(User user) {
		return new RegisterResponse(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				user.getCreatedAt());
	}
}

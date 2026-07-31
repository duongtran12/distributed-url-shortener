package com.duong.url_shortener.auth;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn) {
}

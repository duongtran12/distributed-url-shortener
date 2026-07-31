package com.duong.url_shortener.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password) {

	public LoginRequest {
		email = email == null ? null : email.strip();
	}
}

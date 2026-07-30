package com.duong.url_shortener.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotBlank @Size(min = 2, max = 100) String displayName) {

	public RegisterRequest {
		email = email == null ? null : email.strip();
		displayName = displayName == null ? null : displayName.strip();
	}
}

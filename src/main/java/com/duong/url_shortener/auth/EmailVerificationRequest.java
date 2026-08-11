package com.duong.url_shortener.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
		@NotBlank @Email @Size(max = 320) String email) {

	public EmailVerificationRequest {
		email = email == null ? null : email.strip();
	}
}

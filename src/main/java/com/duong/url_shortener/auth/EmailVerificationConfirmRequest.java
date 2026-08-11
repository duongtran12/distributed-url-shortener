package com.duong.url_shortener.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(
		@NotBlank @Size(max = 128) String token) {
}

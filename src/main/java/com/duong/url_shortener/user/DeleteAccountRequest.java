package com.duong.url_shortener.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
		@NotBlank @Size(max = 72) String currentPassword) {
}

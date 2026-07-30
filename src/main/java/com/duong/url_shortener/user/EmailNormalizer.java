package com.duong.url_shortener.user;

import java.util.Locale;

public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	public static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}

package com.duong.url_shortener.shorturl;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RandomBase62ShortCodeGenerator implements ShortCodeGenerator {

	static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private final RandomGenerator random;
	private final int codeLength;

	@Autowired
	public RandomBase62ShortCodeGenerator(ShortUrlProperties properties) {
		this(new SecureRandom(), properties);
	}

	RandomBase62ShortCodeGenerator(RandomGenerator random, ShortUrlProperties properties) {
		this.random = random;
		this.codeLength = properties.codeLength();
	}

	@Override
	public String generate() {
		StringBuilder code = new StringBuilder(codeLength);
		for (int index = 0; index < codeLength; index++) {
			code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}
}

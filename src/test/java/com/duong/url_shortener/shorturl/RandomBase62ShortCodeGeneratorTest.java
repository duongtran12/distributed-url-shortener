package com.duong.url_shortener.shorturl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

class RandomBase62ShortCodeGeneratorTest {

	@Test
	void shouldGenerateConfiguredLengthUsingBase62Alphabet() {
		RandomGenerator random = mock(RandomGenerator.class);
		when(random.nextInt(RandomBase62ShortCodeGenerator.ALPHABET.length()))
				.thenReturn(0, 9, 10, 35, 36, 61, 1);
		ShortCodeGenerator generator = new RandomBase62ShortCodeGenerator(
				random,
				new ShortUrlProperties(7, 5, "http://localhost:8080"));

		String code = generator.generate();

		assertThat(code).isEqualTo("09AZaz1");
		assertThat(code).matches("[0-9A-Za-z]{7}");
	}

	@Test
	void shouldGenerateAFreshCodeOnEveryCall() {
		RandomGenerator random = mock(RandomGenerator.class);
		when(random.nextInt(RandomBase62ShortCodeGenerator.ALPHABET.length()))
				.thenReturn(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1);
		ShortCodeGenerator generator = new RandomBase62ShortCodeGenerator(
				random,
				new ShortUrlProperties(7, 5, "http://localhost:8080"));

		String firstCode = generator.generate();
		String secondCode = generator.generate();

		assertThat(firstCode).isEqualTo("0000000");
		assertThat(secondCode).isEqualTo("1111111");
	}
}

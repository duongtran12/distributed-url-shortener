package com.duong.url_shortener.shorturl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RedirectServiceTest {

	@Test
	void shouldReturnCacheHitWithoutQueryingPostgres() {
		ShortUrlRepository repository = mock(ShortUrlRepository.class);
		RedirectCache cache = mock(RedirectCache.class);
		when(cache.get("cached1")).thenReturn(Optional.of("https://example.com/cached"));

		URI result = new RedirectService(repository, cache).resolve("cached1");

		assertEquals(URI.create("https://example.com/cached"), result);
		verifyNoInteractions(repository);
	}

	@Test
	void shouldPopulateCacheAfterPostgresLookup() {
		ShortUrlRepository repository = mock(ShortUrlRepository.class);
		RedirectCache cache = mock(RedirectCache.class);
		ShortUrl shortUrl = ShortUrl.create(
				null,
				"miss001",
				"https://example.com/database",
				false,
				null);
		when(cache.get("miss001")).thenReturn(Optional.empty());
		when(repository.findByShortCode("miss001")).thenReturn(Optional.of(shortUrl));

		URI result = new RedirectService(repository, cache).resolve("miss001");

		assertEquals(URI.create("https://example.com/database"), result);
		verify(cache).put(
				eq("miss001"),
				eq("https://example.com/database"),
				eq(null),
				any(Instant.class));
	}
}

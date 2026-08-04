package com.duong.url_shortener.click;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VisitorFingerprintServiceTest {

	private final VisitorFingerprintService service = new VisitorFingerprintService(
			new VisitorFingerprintProperties("test-secret-that-is-at-least-32-characters"));

	@Test
	void shouldCreateStableNonReversibleFingerprint() {
		String first = service.hash("203.0.113.10");
		String repeated = service.hash("203.0.113.10");
		String anotherVisitor = service.hash("203.0.113.11");

		assertEquals(64, first.length());
		assertEquals(first, repeated);
		assertNotEquals(first, anotherVisitor);
		assertNotEquals("203.0.113.10", first);
	}

	@Test
	void shouldIgnoreMissingClientAddress() {
		assertNull(service.hash(null));
		assertNull(service.hash("  "));
	}
}

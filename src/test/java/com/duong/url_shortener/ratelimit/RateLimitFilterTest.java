package com.duong.url_shortener.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.duong.url_shortener.click.VisitorFingerprintService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.ObjectMapper;

class RateLimitFilterTest {

	private final RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
	private final VisitorFingerprintService fingerprintService =
			mock(VisitorFingerprintService.class);
	private final RateLimitProperties properties = new RateLimitProperties(
			true, true, Duration.ofMinutes(1), 10, 60, 120);
	private final RateLimitFilter filter = new RateLimitFilter(
			rateLimiter, properties, fingerprintService, new ObjectMapper());

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldRejectExceededAuthenticationLimit() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST", "/api/v1/auth/login");
		request.setRemoteAddr("203.0.113.10");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		when(fingerprintService.hash("203.0.113.10")).thenReturn("visitor-hash");
		when(rateLimiter.check("auth", "visitor:visitor-hash", 10))
				.thenReturn(new RateLimitDecision(false, 10, 0, 60));

		filter.doFilter(request, response, chain);

		assertEquals(429, response.getStatus());
		assertEquals("10", response.getHeader("RateLimit-Limit"));
		assertEquals("0", response.getHeader("RateLimit-Remaining"));
		assertEquals("60", response.getHeader("Retry-After"));
		assertEquals(true, response.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
		verify(chain, never()).doFilter(request, response);
	}

	@Test
	void shouldAllowRequestsWithinRedirectLimit() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/abc1234");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		when(fingerprintService.hash(anyString())).thenReturn("visitor-hash");
		when(rateLimiter.check(eq("redirect"), eq("visitor:visitor-hash"), eq(60)))
				.thenReturn(new RateLimitDecision(true, 60, 59, 60));

		filter.doFilter(request, response, chain);

		assertEquals("59", response.getHeader("RateLimit-Remaining"));
		verify(chain).doFilter(request, response);
	}

	@Test
	void shouldUseNumericJwtUserIdForAuthenticatedApiQuota() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		Jwt jwt = new Jwt(
				"token",
				Instant.now(),
				Instant.now().plusSeconds(900),
				Map.of("alg", "HS256"),
				Map.of("sub", "user@example.com", "uid", 42L));
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
		when(rateLimiter.check("api", "user:42", 120))
				.thenReturn(new RateLimitDecision(true, 120, 119, 60));

		filter.doFilter(request, response, chain);

		assertEquals("119", response.getHeader("RateLimit-Remaining"));
		verify(chain).doFilter(request, response);
	}
}

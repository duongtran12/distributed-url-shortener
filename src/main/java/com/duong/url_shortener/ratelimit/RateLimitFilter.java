package com.duong.url_shortener.ratelimit;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.duong.url_shortener.click.VisitorFingerprintService;
import com.duong.url_shortener.common.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String RATE_LIMIT = "RateLimit-Limit";
	private static final String RATE_LIMIT_REMAINING = "RateLimit-Remaining";

	private final RedisRateLimiter rateLimiter;
	private final RateLimitProperties properties;
	private final VisitorFingerprintService visitorFingerprintService;
	private final ObjectMapper objectMapper;

	public RateLimitFilter(
			RedisRateLimiter rateLimiter,
			RateLimitProperties properties,
			VisitorFingerprintService visitorFingerprintService,
			ObjectMapper objectMapper) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.visitorFingerprintService = visitorFingerprintService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !properties.enabled() || policyFor(request) == null;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Policy policy = policyFor(request);
		if (policy == null) {
			filterChain.doFilter(request, response);
			return;
		}

		RateLimitDecision decision = rateLimiter.check(
				policy.bucket(), identity(request), policy.limit());
		if (decision.limit() > 0) {
			response.setHeader(RATE_LIMIT, Long.toString(decision.limit()));
			response.setHeader(RATE_LIMIT_REMAINING, Long.toString(decision.remaining()));
		}

		if (decision.allowed()) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
				Instant.now(),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				"RATE_LIMIT_EXCEEDED",
				"Too many requests; retry after the indicated delay",
				request.getRequestURI(),
				List.of()));
	}

	private Policy policyFor(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")) {
			return new Policy("auth", properties.authRequests());
		}
		if (path.startsWith("/api/")) {
			return new Policy("api", properties.apiRequests());
		}
		if (request.getMethod().equals("GET") && path.matches("/[^/]+")) {
			return new Policy("redirect", properties.redirectRequests());
		}
		return null;
	}

	private String identity(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			Object userId = jwt.getClaim("uid");
			return "user:" + String.valueOf(userId);
		}
		String visitorHash = visitorFingerprintService.hash(request.getRemoteAddr());
		return "visitor:" + (visitorHash == null ? "unknown" : visitorHash);
	}

	private record Policy(String bucket, int limit) {
	}
}

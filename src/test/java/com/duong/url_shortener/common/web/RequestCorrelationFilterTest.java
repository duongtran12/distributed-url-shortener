package com.duong.url_shortener.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

	private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void shouldPreserveValidIncomingRequestIdAndExposeItDuringRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls");
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, " edge-request_123 ");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
		FilterChain chain = (ignoredRequest, ignoredResponse) -> requestIdInsideChain.set(
				MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
				.isEqualTo("edge-request_123");
		assertThat(requestIdInsideChain).hasValue("edge-request_123");
		assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	@Test
	void shouldGenerateUuidWhenRequestIdIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
				.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
	}

	@Test
	void shouldReplaceUnsafeIncomingRequestId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls");
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "line-one\r\nforged-header");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
				.matches("[0-9a-f-]{36}")
				.doesNotContain("forged-header");
		assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
	}
}

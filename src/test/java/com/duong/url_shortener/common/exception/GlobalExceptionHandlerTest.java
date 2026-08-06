package com.duong.url_shortener.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duong.url_shortener.ratelimit.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = ErrorHandlingTestController.class,
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = RateLimitFilter.class),
		properties = {
				"debug=false",
				"app.visitor-fingerprint.secret=test-visitor-fingerprint-secret-with-32-characters"
		})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnFieldErrorsForInvalidRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"value\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/test/validation"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
	}

	@Test
	void shouldReturnSafeMessageForMalformedJson() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{invalid-json}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
	}

	@Test
	void shouldMapApiExceptionWithoutExposingImplementationDetails() throws Exception {
		mockMvc.perform(post("/test/business"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.code").value("SHORT_CODE_CONFLICT"))
				.andExpect(jsonPath("$.message").value("Short code already exists"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void shouldReturnBadRequestForInvalidEnumParameter() throws Exception {
		mockMvc.perform(get("/test/enum").param("sort", "POPULAR"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
				.andExpect(jsonPath("$.message").value("Request parameter 'sort' has an invalid value"));
	}

}

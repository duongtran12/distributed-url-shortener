package com.duong.url_shortener.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorHandlingTestController {

	@PostMapping("/test/validation")
	void validate(@Valid @RequestBody TestRequest request) {
	}

	@PostMapping("/test/business")
	void businessError() {
		throw new ApiException(HttpStatus.CONFLICT, "SHORT_CODE_CONFLICT", "Short code already exists");
	}

	record TestRequest(@NotBlank String value) {
	}
}

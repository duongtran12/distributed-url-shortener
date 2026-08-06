package com.duong.url_shortener.common.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(
			ApiException exception,
			HttpServletRequest request) {
		return buildResponse(
				exception.getStatus(),
				exception.getCode(),
				exception.getMessage(),
				request,
				List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationException(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<FieldValidationError> fieldErrors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
				.toList();

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Request validation failed",
				request,
				fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		List<FieldValidationError> fieldErrors = exception.getConstraintViolations()
				.stream()
				.map(violation -> new FieldValidationError(
						violation.getPropertyPath().toString(),
						violation.getMessage()))
				.sorted(Comparator.comparing(FieldValidationError::field))
				.toList();

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"CONSTRAINT_VIOLATION",
				"Request constraint validation failed",
				request,
				fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"Request body is missing or malformed",
				request,
				List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
			MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"INVALID_REQUEST_PARAMETER",
				"Request parameter '%s' has an invalid value".formatted(exception.getName()),
				request,
				List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request) {
		log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), exception);

		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_SERVER_ERROR",
				"An unexpected error occurred",
				request,
				List.of());
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request,
			List<FieldValidationError> fieldErrors) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				code,
				message,
				request.getRequestURI(),
				fieldErrors);

		return ResponseEntity.status(status).body(response);
	}
}

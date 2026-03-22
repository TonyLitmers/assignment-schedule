package com.keyloop.scheduler.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NoAvailabilityException.class)
	public ResponseEntity<ErrorResponse> handleNoAvailability(NoAvailabilityException ex) {
		log.warn("No availability: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.CONFLICT)
			.body(new ErrorResponse("NO_AVAILABILITY", ex.getMessage()));
	}

	@ExceptionHandler(AppointmentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAppointmentNotFound(AppointmentNotFoundException ex) {
		log.warn("Appointment not found: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred."));
	}
}

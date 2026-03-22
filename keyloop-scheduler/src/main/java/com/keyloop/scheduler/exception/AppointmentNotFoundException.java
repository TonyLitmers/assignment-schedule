package com.keyloop.scheduler.exception;

public class AppointmentNotFoundException extends RuntimeException {

	public AppointmentNotFoundException(String message) {
		super(message);
	}
}

package com.keyloop.scheduler.api;

import com.keyloop.scheduler.api.dto.AppointmentResponse;
import com.keyloop.scheduler.api.dto.CreateAppointmentRequest;
import com.keyloop.scheduler.service.AppointmentBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Appointments", description = "Service appointment booking API")
public class AppointmentController {

	private final AppointmentBookingService bookingService;

	public AppointmentController(AppointmentBookingService bookingService) {
		this.bookingService = bookingService;
	}

	@Operation(summary = "Create appointment", description = "Book a service appointment with vehicle, service type, dealership and desired time")
	@PostMapping("/appointments")
	public ResponseEntity<AppointmentResponse> createAppointment(
		@Valid @RequestBody CreateAppointmentRequest request
	) {
		AppointmentResponse response = bookingService.createAppointment(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get appointment", description = "Retrieve appointment details by ID")
	@GetMapping("/appointments/{id}")
	public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable("id") Long id) {
		AppointmentResponse response = bookingService.getAppointment(id);
		return ResponseEntity.ok(response);
	}
}

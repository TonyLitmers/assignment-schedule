package com.keyloop.scheduler.api.dto;

import java.time.OffsetDateTime;

public record AppointmentResponse(
	Long appointmentId,
	String status,
	Long customerId,
	Long vehicleId,
	Long serviceTypeId,
	Long serviceBayId,
	Long technicianId,
	OffsetDateTime startTime,
	OffsetDateTime endTime
) {}

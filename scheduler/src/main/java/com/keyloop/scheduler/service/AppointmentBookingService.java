package com.keyloop.scheduler.service;

import com.keyloop.scheduler.api.dto.AppointmentResponse;
import com.keyloop.scheduler.api.dto.CreateAppointmentRequest;
import com.keyloop.scheduler.exception.NoAvailabilityException;

public interface AppointmentBookingService {

	AppointmentResponse createAppointment(CreateAppointmentRequest request) throws NoAvailabilityException;

	AppointmentResponse getAppointment(Long appointmentId);
}


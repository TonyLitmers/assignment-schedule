package com.keyloop.scheduler.service;

import com.keyloop.scheduler.api.dto.AppointmentResponse;
import com.keyloop.scheduler.api.dto.CreateAppointmentRequest;
import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.domain.Customer;
import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.ServiceType;
import com.keyloop.scheduler.domain.Technician;
import com.keyloop.scheduler.domain.Vehicle;
import com.keyloop.scheduler.exception.AppointmentNotFoundException;
import com.keyloop.scheduler.exception.NoAvailabilityException;
import com.keyloop.scheduler.repository.AppointmentRepository;
import com.keyloop.scheduler.repository.CustomerRepository;
import com.keyloop.scheduler.repository.DealershipRepository;
import com.keyloop.scheduler.repository.ServiceBayRepository;
import com.keyloop.scheduler.repository.ServiceTypeRepository;
import com.keyloop.scheduler.repository.TechnicianRepository;
import com.keyloop.scheduler.repository.VehicleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentBookingServiceImpl implements AppointmentBookingService {

	private final AppointmentRepository appointmentRepository;
	private final ServiceTypeRepository serviceTypeRepository;
	private final ServiceBayRepository serviceBayRepository;
	private final TechnicianRepository technicianRepository;
	private final CustomerRepository customerRepository;
	private final VehicleRepository vehicleRepository;
	private final DealershipRepository dealershipRepository;

	public AppointmentBookingServiceImpl(
		AppointmentRepository appointmentRepository,
		ServiceTypeRepository serviceTypeRepository,
		ServiceBayRepository serviceBayRepository,
		TechnicianRepository technicianRepository,
		CustomerRepository customerRepository,
		VehicleRepository vehicleRepository,
		DealershipRepository dealershipRepository
	) {
		this.appointmentRepository = appointmentRepository;
		this.serviceTypeRepository = serviceTypeRepository;
		this.serviceBayRepository = serviceBayRepository;
		this.technicianRepository = technicianRepository;
		this.customerRepository = customerRepository;
		this.vehicleRepository = vehicleRepository;
		this.dealershipRepository = dealershipRepository;
	}

	@Override
	@Transactional
	public AppointmentResponse createAppointment(CreateAppointmentRequest request) throws NoAvailabilityException {
		ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
			.orElseThrow(() -> new IllegalArgumentException("Unknown serviceTypeId: " + request.serviceTypeId()));

		Dealership dealership = dealershipRepository.findById(request.dealershipId())
			.orElseThrow(() -> new IllegalArgumentException("Unknown dealershipId: " + request.dealershipId()));

		Customer customer = customerRepository.findById(request.customerId())
			.orElseThrow(() -> new IllegalArgumentException("Unknown customerId: " + request.customerId()));

		Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
			.orElseThrow(() -> new IllegalArgumentException("Unknown vehicleId: " + request.vehicleId()));

		if (!vehicle.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalArgumentException("Vehicle does not belong to customer.");
		}

		OffsetDateTime start = request.desiredStartTime();
		OffsetDateTime end = start.plusMinutes(serviceType.getDurationMinutes());

		AppointmentStatus status = AppointmentStatus.CONFIRMED;

		List<Long> availableBayIds = serviceBayRepository.findAvailableServiceBayIds(
			request.dealershipId(),
			start,
			end,
			status
		);

		if (availableBayIds.isEmpty()) {
			throw new NoAvailabilityException("No service bay available for the requested time window.");
		}

		List<Long> availableTechnicianIds = technicianRepository.findAvailableTechnicianIds(
			request.dealershipId(),
			start,
			end,
			status
		);

		if (availableTechnicianIds.isEmpty()) {
			throw new NoAvailabilityException("No technician available for the requested time window.");
		}

		for (Long bayId : availableBayIds) {
			ServiceBay lockedBay = serviceBayRepository.findByIdForUpdate(bayId)
				.orElseThrow(() -> new IllegalStateException("ServiceBay disappeared: id=" + bayId));

			if (appointmentRepository.countOverlappingForServiceBay(bayId, start, end, status) > 0) {
				continue;
			}

			for (Long technicianId : availableTechnicianIds) {
				Technician lockedTechnician = technicianRepository.findByIdForUpdate(technicianId)
					.orElseThrow(() -> new IllegalStateException("Technician disappeared: id=" + technicianId));

				if (appointmentRepository.countOverlappingForTechnician(technicianId, start, end, status) > 0) {
					continue;
				}

				Appointment appointment = new Appointment();
				appointment.setStatus(status);
				appointment.setStartTime(start);
				appointment.setEndTime(end);
				appointment.setCustomer(customer);
				appointment.setVehicle(vehicle);
				appointment.setServiceType(serviceType);
				appointment.setServiceBay(lockedBay);
				appointment.setTechnician(lockedTechnician);

				Appointment saved = appointmentRepository.save(appointment);

				return new AppointmentResponse(
					saved.getId(),
					status.name(),
					saved.getCustomer().getId(),
					saved.getVehicle().getId(),
					saved.getServiceType().getId(),
					saved.getServiceBay().getId(),
					saved.getTechnician().getId(),
					saved.getStartTime(),
					saved.getEndTime()
				);
			}
		}

		throw new NoAvailabilityException("No combination of service bay + technician can accommodate the requested duration.");
	}

	@Override
	public AppointmentResponse getAppointment(Long appointmentId) {
		Appointment appt = appointmentRepository.findById(appointmentId)
			.orElseThrow(() -> new AppointmentNotFoundException("Unknown appointmentId: " + appointmentId));

		return new AppointmentResponse(
			appt.getId(),
			appt.getStatus().name(),
			appt.getCustomer().getId(),
			appt.getVehicle().getId(),
			appt.getServiceType().getId(),
			appt.getServiceBay().getId(),
			appt.getTechnician().getId(),
			appt.getStartTime(),
			appt.getEndTime()
		);
	}
}

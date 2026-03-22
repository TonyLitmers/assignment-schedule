package com.keyloop.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.keyloop.scheduler.exception.NoAvailabilityException;
import com.keyloop.scheduler.repository.AppointmentRepository;
import com.keyloop.scheduler.repository.CustomerRepository;
import com.keyloop.scheduler.repository.DealershipRepository;
import com.keyloop.scheduler.repository.ServiceBayRepository;
import com.keyloop.scheduler.repository.ServiceTypeRepository;
import com.keyloop.scheduler.repository.TechnicianRepository;
import com.keyloop.scheduler.repository.VehicleRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AppointmentBookingServiceTest {

	@Autowired
	AppointmentBookingService bookingService;

	@Autowired
	AppointmentRepository appointmentRepository;

	@Autowired
	DealershipRepository dealershipRepository;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	VehicleRepository vehicleRepository;

	@Autowired
	ServiceTypeRepository serviceTypeRepository;

	@Autowired
	ServiceBayRepository serviceBayRepository;

	@Autowired
	TechnicianRepository technicianRepository;

	private static OffsetDateTime t(String iso) {
		return OffsetDateTime.parse(iso);
	}

	@Test
	void createsAppointmentWhenResourcesAvailable() {
		Dealership dealership = dealershipRepository.save(new Dealership("D1"));
		Customer customer = customerRepository.save(new Customer("C1"));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(customer, "ABC-123", "Toyota"));
		ServiceType oilChange = serviceTypeRepository.save(new ServiceType("Oil change", 60));

		ServiceBay bay = serviceBayRepository.save(new ServiceBay("Bay-1", dealership));
		Technician technician = technicianRepository.save(new Technician("Tech-1", dealership));

		OffsetDateTime desiredStart = t("2026-03-20T10:00:00+07:00");

		CreateAppointmentRequest request = new CreateAppointmentRequest(
			customer.getId(),
			vehicle.getId(),
			dealership.getId(),
			oilChange.getId(),
			desiredStart
		);

		AppointmentResponse response = bookingService.createAppointment(request);
		assertNotNull(response);
		assertEquals("CONFIRMED", response.status());
		assertNotNull(response.appointmentId());
		assertEquals(desiredStart, response.startTime());
		assertEquals(desiredStart.plusMinutes(60), response.endTime());
		assertEquals(bay.getId(), response.serviceBayId());
		assertEquals(technician.getId(), response.technicianId());

		appointmentRepository.findById(response.appointmentId())
			.ifPresent(a -> {
				assertEquals(AppointmentStatus.CONFIRMED, a.getStatus());
				assertEquals(desiredStart.plusMinutes(60), a.getEndTime());
			});
	}

	@Test
	void failsWhenServiceBayOverlaps() {
		Dealership dealership = dealershipRepository.save(new Dealership("D1"));
		Customer customer = customerRepository.save(new Customer("C1"));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(customer, "ABC-123", "Toyota"));
		ServiceType oilChange = serviceTypeRepository.save(new ServiceType("Oil change", 60));

		ServiceBay bay = serviceBayRepository.save(new ServiceBay("Bay-1", dealership));
		Technician technician = technicianRepository.save(new Technician("Tech-1", dealership));

		OffsetDateTime desiredStart = t("2026-03-20T10:00:00+07:00");
		OffsetDateTime existingStart = desiredStart.minusMinutes(30);
		OffsetDateTime existingEnd = desiredStart.plusMinutes(30); // overlaps

		Appointment existing = new Appointment();
		existing.setStatus(AppointmentStatus.CONFIRMED);
		existing.setStartTime(existingStart);
		existing.setEndTime(existingEnd);
		existing.setCustomer(customer);
		existing.setVehicle(vehicle);
		existing.setServiceType(oilChange);
		existing.setServiceBay(bay);
		existing.setTechnician(technician);
		appointmentRepository.save(existing);

		CreateAppointmentRequest request = new CreateAppointmentRequest(
			customer.getId(),
			vehicle.getId(),
			dealership.getId(),
			oilChange.getId(),
			desiredStart
		);

		assertThrows(NoAvailabilityException.class, () -> bookingService.createAppointment(request));
	}

	@Test
	void failsWhenVehicleNotOwnedByCustomer() {
		Dealership dealership = dealershipRepository.save(new Dealership("D1"));
		Customer customer1 = customerRepository.save(new Customer("C1"));
		Customer customer2 = customerRepository.save(new Customer("C2"));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(customer1, "ABC-123", "Toyota"));
		ServiceType oilChange = serviceTypeRepository.save(new ServiceType("Oil change", 60));

		serviceBayRepository.save(new ServiceBay("Bay-1", dealership));
		technicianRepository.save(new Technician("Tech-1", dealership));

		// customer2 tries to book vehicle that belongs to customer1
		OffsetDateTime desiredStart = t("2026-03-20T10:00:00+07:00");
		CreateAppointmentRequest request = new CreateAppointmentRequest(
			customer2.getId(),
			vehicle.getId(),
			dealership.getId(),
			oilChange.getId(),
			desiredStart
		);

		assertThrows(IllegalArgumentException.class, () -> bookingService.createAppointment(request));
	}
}


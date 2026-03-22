package com.keyloop.scheduler.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keyloop.scheduler.api.dto.CreateAppointmentRequest;
import com.keyloop.scheduler.api.dto.AppointmentResponse;
import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.domain.Customer;
import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.ServiceType;
import com.keyloop.scheduler.domain.Technician;
import com.keyloop.scheduler.domain.Vehicle;
import com.keyloop.scheduler.repository.AppointmentRepository;
import com.keyloop.scheduler.repository.CustomerRepository;
import com.keyloop.scheduler.repository.DealershipRepository;
import com.keyloop.scheduler.repository.ServiceBayRepository;
import com.keyloop.scheduler.repository.ServiceTypeRepository;
import com.keyloop.scheduler.repository.TechnicianRepository;
import com.keyloop.scheduler.repository.VehicleRepository;
import com.keyloop.scheduler.service.AppointmentBookingService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AppointmentControllerTest {

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext webApplicationContext;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

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
	void createAppointment_returns201() throws Exception {
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

		String jsonRequest = String.format("""
			{
			  "customerId": %d,
			  "vehicleId": %d,
			  "dealershipId": %d,
			  "serviceTypeId": %d,
			  "desiredStartTime": "%s"
			}
			""",
			request.customerId(),
			request.vehicleId(),
			request.dealershipId(),
			request.serviceTypeId(),
			request.desiredStartTime().toString()
		);

		mockMvc.perform(
			post("/api/appointments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest)
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status", is("CONFIRMED")))
				.andExpect(jsonPath("$.startTime").exists());

		// Basic sanity: record should exist in DB.
		long count = appointmentRepository.count();
		org.junit.jupiter.api.Assertions.assertTrue(count > 0);
	}

	@Test
	void createAppointment_overlappingBayOrTech_returns409() throws Exception {
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

		String jsonRequest = String.format("""
			{
			  "customerId": %d,
			  "vehicleId": %d,
			  "dealershipId": %d,
			  "serviceTypeId": %d,
			  "desiredStartTime": "%s"
			}
			""",
			request.customerId(),
			request.vehicleId(),
			request.dealershipId(),
			request.serviceTypeId(),
			request.desiredStartTime().toString()
		);

		mockMvc.perform(
			post("/api/appointments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest)
		)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code", is("NO_AVAILABILITY")));
	}

	@Test
	void getAppointment_returns200Or404() throws Exception {
		Dealership dealership = dealershipRepository.save(new Dealership("D1"));
		Customer customer = customerRepository.save(new Customer("C1"));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(customer, "ABC-123", "Toyota"));
		ServiceType oilChange = serviceTypeRepository.save(new ServiceType("Oil change", 60));

		serviceBayRepository.save(new ServiceBay("Bay-1", dealership));
		serviceBayRepository.save(new ServiceBay("Bay-2", dealership));
		technicianRepository.save(new Technician("Tech-1", dealership));

		OffsetDateTime desiredStart = t("2026-03-20T10:00:00+07:00");
		CreateAppointmentRequest request = new CreateAppointmentRequest(
			customer.getId(),
			vehicle.getId(),
			dealership.getId(),
			oilChange.getId(),
			desiredStart
		);

		AppointmentResponse created = bookingService.createAppointment(request);

		mockMvc.perform(get("/api/appointments/" + created.appointmentId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.appointmentId", is(created.appointmentId().intValue())));

		mockMvc.perform(get("/api/appointments/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code", is("NOT_FOUND")));
	}
}


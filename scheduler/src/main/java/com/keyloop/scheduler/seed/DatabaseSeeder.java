package com.keyloop.scheduler.seed;

import com.keyloop.scheduler.domain.Customer;
import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.ServiceType;
import com.keyloop.scheduler.domain.Technician;
import com.keyloop.scheduler.domain.Vehicle;
import com.keyloop.scheduler.repository.CustomerRepository;
import com.keyloop.scheduler.repository.DealershipRepository;
import com.keyloop.scheduler.repository.ServiceBayRepository;
import com.keyloop.scheduler.repository.ServiceTypeRepository;
import com.keyloop.scheduler.repository.TechnicianRepository;
import com.keyloop.scheduler.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

	private final DealershipRepository dealershipRepository;
	private final CustomerRepository customerRepository;
	private final VehicleRepository vehicleRepository;
	private final ServiceTypeRepository serviceTypeRepository;
	private final ServiceBayRepository serviceBayRepository;
	private final TechnicianRepository technicianRepository;

	public DatabaseSeeder(
		DealershipRepository dealershipRepository,
		CustomerRepository customerRepository,
		VehicleRepository vehicleRepository,
		ServiceTypeRepository serviceTypeRepository,
		ServiceBayRepository serviceBayRepository,
		TechnicianRepository technicianRepository
	) {
		this.dealershipRepository = dealershipRepository;
		this.customerRepository = customerRepository;
		this.vehicleRepository = vehicleRepository;
		this.serviceTypeRepository = serviceTypeRepository;
		this.serviceBayRepository = serviceBayRepository;
		this.technicianRepository = technicianRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (dealershipRepository.count() > 0) {
			log.debug("Database already seeded, skipping.");
			return;
		}

		log.info("Seeding database with initial data...");

		// Dealerships
		Dealership hanoi = dealershipRepository.save(new Dealership("Keyloop Hanoi", "123 Main Street, Hanoi"));
		Dealership hcmc = dealershipRepository.save(new Dealership("Keyloop Ho Chi Minh City", "456 Central Avenue, Ho Chi Minh City"));

		// Customers
		Customer c1 = customerRepository.save(new Customer("John Smith", "+1-555-0101", "john.smith@email.com"));
		Customer c2 = customerRepository.save(new Customer("Jane Doe", "+1-555-0102", "jane.doe@email.com"));
		Customer c3 = customerRepository.save(new Customer("Bob Johnson", "+1-555-0103", "bob.johnson@email.com"));

		// Vehicles
		vehicleRepository.save(new Vehicle(c1, "ABC-1234", "Toyota Vios"));
		vehicleRepository.save(new Vehicle(c1, "DEF-5678", "Honda City"));
		vehicleRepository.save(new Vehicle(c2, "GHI-9012", "Mazda 3"));
		vehicleRepository.save(new Vehicle(c3, "JKL-3456", "Ford Ranger"));

		// Service types
		serviceTypeRepository.save(new ServiceType("Oil change", 30));
		serviceTypeRepository.save(new ServiceType("Engine repair", 120));
		serviceTypeRepository.save(new ServiceType("Full service", 90));
		serviceTypeRepository.save(new ServiceType("Brake check", 45));
		serviceTypeRepository.save(new ServiceType("Tire change", 60));

		// Service bays
		serviceBayRepository.save(new ServiceBay("Bay-1", hanoi));
		serviceBayRepository.save(new ServiceBay("Bay-2", hanoi));
		serviceBayRepository.save(new ServiceBay("Bay-3", hanoi));
		serviceBayRepository.save(new ServiceBay("Bay-1", hcmc));
		serviceBayRepository.save(new ServiceBay("Bay-2", hcmc));

		// Technicians
		technicianRepository.save(new Technician("Mike Taylor", hanoi));
		technicianRepository.save(new Technician("Sarah Wilson", hanoi));
		technicianRepository.save(new Technician("David Brown", hanoi));
		technicianRepository.save(new Technician("Chris Davis", hcmc));
		technicianRepository.save(new Technician("Emily Miller", hcmc));

		log.info("Database seeded successfully.");
	}
}

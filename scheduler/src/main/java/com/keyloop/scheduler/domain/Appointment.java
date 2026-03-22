package com.keyloop.scheduler.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "appointment")
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private AppointmentStatus status;

	private OffsetDateTime startTime;

	private OffsetDateTime endTime;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_type_id", nullable = false)
	private ServiceType serviceType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_bay_id", nullable = false)
	private ServiceBay serviceBay;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "technician_id", nullable = false)
	private Technician technician;

	public Appointment() {
	}

	public Long getId() {
		return id;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public OffsetDateTime getEndTime() {
		return endTime;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public ServiceType getServiceType() {
		return serviceType;
	}

	public ServiceBay getServiceBay() {
		return serviceBay;
	}

	public Technician getTechnician() {
		return technician;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	public void setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(OffsetDateTime endTime) {
		this.endTime = endTime;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public void setServiceType(ServiceType serviceType) {
		this.serviceType = serviceType;
	}

	public void setServiceBay(ServiceBay serviceBay) {
		this.serviceBay = serviceBay;
	}

	public void setTechnician(Technician technician) {
		this.technician = technician;
	}
}

package com.keyloop.scheduler.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_type")
public class ServiceType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	// Duration in minutes
	private long durationMinutes;

	public ServiceType() {
	}

	public ServiceType(String name, long durationMinutes) {
		this.name = name;
		this.durationMinutes = durationMinutes;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getDurationMinutes() {
		return durationMinutes;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDurationMinutes(long durationMinutes) {
		this.durationMinutes = durationMinutes;
	}
}

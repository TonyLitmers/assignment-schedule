package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.AppointmentStatus;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	@Query("""
		select count(a)
		from Appointment a
		where a.serviceBay.id = :serviceBayId
		and a.status = :status
		and a.startTime < :endTime
		and a.endTime > :startTime
		""")
	long countOverlappingForServiceBay(
		@Param("serviceBayId") Long serviceBayId,
		@Param("startTime") OffsetDateTime startTime,
		@Param("endTime") OffsetDateTime endTime,
		@Param("status") AppointmentStatus status
	);

	@Query("""
		select count(a)
		from Appointment a
		where a.technician.id = :technicianId
		and a.status = :status
		and a.startTime < :endTime
		and a.endTime > :startTime
		""")
	long countOverlappingForTechnician(
		@Param("technicianId") Long technicianId,
		@Param("startTime") OffsetDateTime startTime,
		@Param("endTime") OffsetDateTime endTime,
		@Param("status") AppointmentStatus status
	);
}


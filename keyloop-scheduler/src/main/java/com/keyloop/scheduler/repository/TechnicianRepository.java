package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.domain.Technician;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

	@Query("""
		select t.id
		from Technician t
		where t.dealership.id = :dealershipId
		and not exists (
			select 1
			from Appointment a
			where a.technician.id = t.id
			and a.status = :status
			and a.startTime < :endTime
			and a.endTime > :startTime
		)
		order by t.id
		""")
	List<Long> findAvailableTechnicianIds(
		@Param("dealershipId") Long dealershipId,
		@Param("startTime") OffsetDateTime startTime,
		@Param("endTime") OffsetDateTime endTime,
		@Param("status") AppointmentStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Technician t where t.id = :id")
	Optional<Technician> findByIdForUpdate(@Param("id") Long id);
}

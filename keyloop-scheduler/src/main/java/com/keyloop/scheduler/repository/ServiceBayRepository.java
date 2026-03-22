package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.domain.ServiceBay;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {

	@Query("""
		select b.id
		from ServiceBay b
		where b.dealership.id = :dealershipId
		and not exists (
			select 1
			from Appointment a
			where a.serviceBay.id = b.id
			and a.status = :status
			and a.startTime < :endTime
			and a.endTime > :startTime
		)
		order by b.id
		""")
	List<Long> findAvailableServiceBayIds(
		@Param("dealershipId") Long dealershipId,
		@Param("startTime") OffsetDateTime startTime,
		@Param("endTime") OffsetDateTime endTime,
		@Param("status") AppointmentStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from ServiceBay b where b.id = :id")
	Optional<ServiceBay> findByIdForUpdate(@Param("id") Long id);
}

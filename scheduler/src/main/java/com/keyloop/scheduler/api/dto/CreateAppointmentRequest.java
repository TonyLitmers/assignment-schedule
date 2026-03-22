package com.keyloop.scheduler.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
	@NotNull Long customerId,
	@NotNull Long vehicleId,
	@NotNull Long dealershipId,
	@NotNull Long serviceTypeId,
	@NotNull OffsetDateTime desiredStartTime
) {}


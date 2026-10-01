package com.olarize.employment.infrastructure.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectedEmployeeResponseDto(UUID companyId, UUID employeeId, UUID episodeId,
        long version, long episodeVersion, LocalDate effectiveDate, Instant recordedAt) { }

package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.EmployeeCorrection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface CorrectEmployeeUseCase {
    Result correct(UUID companyId, UUID employeeId, UUID episodeId, long expectedVersion,
            long expectedEpisodeVersion, EmployeeCorrection correction, UUID key, String correlationId);
    record Result(UUID companyId, UUID employeeId, UUID episodeId, long version, long episodeVersion,
            LocalDate effectiveDate, Instant recordedAt) { }
}

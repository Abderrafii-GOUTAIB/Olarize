package com.olarize.employment.application.port.out;

import com.olarize.employment.application.port.in.CorrectEmployeeUseCase.Result;
import com.olarize.employment.domain.model.EmployeeCorrection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeCorrectionPort {
    void serializeRetry(UUID companyId, String actor, UUID key);
    Optional<Receipt> findRetry(UUID companyId, String actor, UUID key);
    Result correct(UUID companyId, UUID employeeId, UUID episodeId, long version, long episodeVersion,
            EmployeeCorrection correction, String actor, UUID key, String hash, String correlationId, Instant time);
    record Receipt(String hash, Result result) { }
}

package com.olarize.employment.application.port.out;

import com.olarize.employment.domain.model.EmployeeCreation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeCreationPort {
    void serializeRetry(UUID companyId, String actor, UUID key);
    Optional<CreationReceipt> findReceipt(UUID companyId, String actor, UUID key);
    CreationReceipt insert(UUID companyId, String actor, UUID key, String payloadHash,
            EmployeeCreation facts, String correlationId, Instant recordedAt);

    record CreationReceipt(UUID employeeId, UUID episodeId, String payloadHash,
            EmployeeCreation facts) { }
}

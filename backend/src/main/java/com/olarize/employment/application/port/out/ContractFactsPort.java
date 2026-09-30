package com.olarize.employment.application.port.out;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.domain.model.ContractFacts;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContractFactsPort {
    void serializeRetry(UUID companyId, String actor, String operation, UUID key);
    Optional<RetryReceipt> findRetry(UUID companyId, String actor, String operation, UUID key);
    ContractVersionResponse create(UUID companyId, UUID employeeId, UUID episodeId,
            String actor, UUID key, String payloadHash, ContractFacts facts,
            String correlationId, Instant recordedAt);
    ContractVersionResponse correct(UUID companyId, UUID employeeId, UUID episodeId,
            UUID contractId, long expectedVersion, String actor, UUID key, String payloadHash,
            ContractFacts facts, String reason, String evidenceReference,
            String correlationId, Instant recordedAt);

    record RetryReceipt(String payloadHash, ContractVersionResponse result) { }
}

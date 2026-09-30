package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.ContractFacts;
import java.util.UUID;

public interface CorrectContractUseCase {
    ContractVersionResponse correct(UUID companyId, UUID employeeId, UUID episodeId,
            UUID contractId, long expectedVersion, ContractFacts facts, String reason,
            String evidenceReference, UUID idempotencyKey, String correlationId);
}

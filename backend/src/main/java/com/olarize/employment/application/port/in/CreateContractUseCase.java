package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.ContractFacts;
import java.util.UUID;

public interface CreateContractUseCase {
    ContractVersionResponse create(UUID companyId, UUID employeeId, UUID episodeId,
            ContractFacts facts, UUID idempotencyKey, String correlationId);
}

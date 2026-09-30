package com.olarize.employment.application.service;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.application.port.in.CorrectContractUseCase;
import com.olarize.employment.application.port.out.ContractFactsPort;
import com.olarize.employment.domain.model.ContractFacts;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class CorrectContractService extends ContractFactsServiceSupport implements CorrectContractUseCase {
    private final ContractFactsPort records;

    public CorrectContractService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, ContractFactsPort records, PlatformTransactionManager transactions) {
        super(access, scope, database, records, transactions);
        this.records = records;
    }

    @Override
    public ContractVersionResponse correct(UUID companyId, UUID employeeId, UUID episodeId,
            UUID contractId, long expectedVersion, ContractFacts facts, String reason,
            String evidenceReference, UUID key, String correlationId) {
        if (employeeId == null || episodeId == null || contractId == null || expectedVersion < 1
                || reason == null || reason.isBlank() || reason.trim().length() > 1000
                || evidenceReference == null || evidenceReference.isBlank()
                || evidenceReference.trim().length() > 200) {
            throw new IllegalArgumentException("Required correction facts are invalid");
        }
        String normalizedReason = reason.trim();
        String normalizedEvidence = evidenceReference.trim();
        return mutate(companyId, "CORRECT", key,
                List.of(employeeId.toString(), episodeId.toString(), contractId.toString(),
                        Long.toString(expectedVersion), normalizedReason, normalizedEvidence),
                facts, (hash, actor) -> records.correct(companyId, employeeId, episodeId,
                        contractId, expectedVersion, actor, key, hash, facts, normalizedReason,
                        normalizedEvidence, correlationId, Instant.now()));
    }
}

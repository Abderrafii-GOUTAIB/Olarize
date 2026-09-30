package com.olarize.employment.application.service;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.application.port.in.CreateContractUseCase;
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
public class CreateContractService extends ContractFactsServiceSupport implements CreateContractUseCase {
    private final ContractFactsPort records;

    public CreateContractService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, ContractFactsPort records, PlatformTransactionManager transactions) {
        super(access, scope, database, records, transactions);
        this.records = records;
    }

    @Override
    public ContractVersionResponse create(UUID companyId, UUID employeeId, UUID episodeId,
            ContractFacts facts, UUID key, String correlationId) {
        if (employeeId == null || episodeId == null) throw new IllegalArgumentException("Employee episode is required");
        return mutate(companyId, "CREATE", key, List.of(employeeId.toString(), episodeId.toString()), facts,
                (hash, actor) -> records.create(companyId, employeeId, episodeId, actor, key,
                        hash, facts, correlationId, Instant.now()));
    }
}

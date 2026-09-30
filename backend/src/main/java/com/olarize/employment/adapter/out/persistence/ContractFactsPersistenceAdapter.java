package com.olarize.employment.adapter.out.persistence;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.application.port.out.ContractFactsPort;
import com.olarize.employment.application.service.ContractFactsConflict;
import com.olarize.employment.application.service.ContractFactsNotFound;
import com.olarize.employment.domain.model.ContractFacts;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
class ContractFactsPersistenceAdapter implements ContractFactsPort {
    private final EmploymentContractJpaRepository contracts;
    private final EmploymentContractVersionJpaRepository versions;
    private final ContractMutationRequestJpaRepository requests;
    private final JdbcTemplate database;

    ContractFactsPersistenceAdapter(EmploymentContractJpaRepository contracts,
            EmploymentContractVersionJpaRepository versions,
            ContractMutationRequestJpaRepository requests, JdbcTemplate database) {
        this.contracts = contracts;
        this.versions = versions;
        this.requests = requests;
        this.database = database;
    }

    @Override
    public void serializeRetry(UUID companyId, String actor, String operation, UUID key) {
        // JPA remains authoritative; this transaction lock closes the same-key retry race.
        database.queryForObject("select pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                companyId + ":" + actor + ":" + operation + ":" + key);
    }

    @Override
    public Optional<RetryReceipt> findRetry(UUID companyId, String actor, String operation, UUID key) {
        return requests.findByCompanyIdAndActorSubjectAndOperationAndIdempotencyKey(
                companyId, actor, operation, key).map(request -> {
                    var version = versions.findByContractIdAndCompanyIdAndVersion(
                            request.contractId, companyId, request.resultVersion).orElseThrow();
                    return new RetryReceipt(request.payloadHash,
                            ContractFactsPersistenceMapper.response(version));
                });
    }

    @Override
    public ContractVersionResponse create(UUID companyId, UUID employeeId, UUID episodeId,
            String actor, UUID key, String payloadHash, ContractFacts facts,
            String correlationId, Instant recordedAt) {
        UUID contractId = UUID.randomUUID();
        var contract = new EmploymentContractJpaEntity(contractId, companyId, employeeId, episodeId);
        contracts.saveAndFlush(contract);
        var version = version(contractId, companyId, employeeId, episodeId, 1,
                facts, actor, "CREATE", null, null, correlationId, recordedAt);
        versions.save(version);
        requests.save(new ContractMutationRequestJpaEntity(UUID.randomUUID(), companyId,
                actor, "CREATE", key, payloadHash, contractId, 1, recordedAt));
        return ContractFactsPersistenceMapper.response(version);
    }

    @Override
    public ContractVersionResponse correct(UUID companyId, UUID employeeId, UUID episodeId,
            UUID contractId, long expectedVersion, String actor, UUID key, String payloadHash,
            ContractFacts facts, String reason, String evidenceReference,
            String correlationId, Instant recordedAt) {
        var contract = contracts.findByIdAndCompanyIdAndEmployeeIdAndEpisodeId(
                contractId, companyId, employeeId, episodeId).orElseThrow(ContractFactsNotFound::new);
        if (contract.currentVersion != expectedVersion) throw new ContractFactsConflict();
        long resultVersion = expectedVersion + 1;
        contract.currentVersion = resultVersion;
        try {
            contracts.saveAndFlush(contract);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException conflict) {
            throw new ContractFactsConflict();
        }
        var version = version(contractId, companyId, employeeId, episodeId, resultVersion,
                facts, actor, "CORRECT", reason, evidenceReference, correlationId, recordedAt);
        versions.saveAndFlush(version);
        requests.save(new ContractMutationRequestJpaEntity(UUID.randomUUID(), companyId,
                actor, "CORRECT", key, payloadHash, contractId, resultVersion, recordedAt));
        return ContractFactsPersistenceMapper.response(version);
    }

    private static EmploymentContractVersionJpaEntity version(UUID contractId, UUID companyId,
            UUID employeeId, UUID episodeId, long version, ContractFacts facts,
            String actor, String operation, String reason, String evidenceReference,
            String correlationId, Instant recordedAt) {
        return new EmploymentContractVersionJpaEntity(UUID.randomUUID(), contractId,
                companyId, employeeId, episodeId, version, facts.typeCode(), facts.startDate(),
                facts.endDate(), facts.sourceReference(), actor, operation, reason,
                evidenceReference, correlationId, recordedAt);
    }
}

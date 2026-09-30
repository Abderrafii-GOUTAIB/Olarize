package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeCreationAuditJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeCreationRequestJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentEpisodeJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.mapper.EmployeePersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeCreationAuditJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeCreationRequestJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmploymentEpisodeJpaRepository;

import com.olarize.employment.application.port.out.EmployeeCreationPort;
import com.olarize.employment.domain.model.EmployeeCreation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class EmployeeCreationPersistenceAdapter implements EmployeeCreationPort {
    private final EmployeeJpaRepository employees;
    private final EmploymentEpisodeJpaRepository episodes;
    private final EmployeeCreationAuditJpaRepository audits;
    private final EmployeeCreationRequestJpaRepository requests;
    private final JdbcTemplate database;

    EmployeeCreationPersistenceAdapter(EmployeeJpaRepository employees,
            EmploymentEpisodeJpaRepository episodes, EmployeeCreationAuditJpaRepository audits,
            EmployeeCreationRequestJpaRepository requests, JdbcTemplate database) {
        this.employees = employees;
        this.episodes = episodes;
        this.audits = audits;
        this.requests = requests;
        this.database = database;
    }

    @Override
    public void serializeRetry(UUID companyId, String actor, UUID key) {
        // A transaction-scoped PostgreSQL advisory lock serializes equal retry keys.
        // JPA CRUD remains authoritative; this focused native lock closes the retry race.
        database.queryForObject("select pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                companyId + ":" + actor + ":" + key);
    }

    @Override
    public Optional<CreationReceipt> findReceipt(UUID companyId, String actor, UUID key) {
        return requests.findByCompanyIdAndActorSubjectAndIdempotencyKey(companyId, actor, key).map(request -> {
            var employee = employees.findByIdAndCompanyId(request.getEmployeeId(), companyId).orElseThrow();
            var episode = episodes.findByEmployeeIdAndCompanyId(employee.getId(), companyId).orElseThrow();
            return new CreationReceipt(employee.getId(), episode.getId(), request.getPayloadHash(),
                    EmployeePersistenceMapper.facts(employee, episode));
        });
    }

    @Override
    public CreationReceipt insert(UUID companyId, String actor, UUID key, String payloadHash,
            EmployeeCreation facts, String correlationId, Instant recordedAt) {
        UUID employeeId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        employees.saveAndFlush(new EmployeeJpaEntity(employeeId, companyId, facts.reference(),
                facts.normalizedReference(), facts.name(), facts.jobTitle(), facts.department(),
                facts.site(), facts.workEmail(), facts.workPhone(), recordedAt));
        episodes.save(new EmploymentEpisodeJpaEntity(episodeId, employeeId, companyId,
                facts.startDate(), recordedAt));
        audits.save(new EmployeeCreationAuditJpaEntity(UUID.randomUUID(), employeeId,
                companyId, actor, correlationId, recordedAt));
        requests.save(new EmployeeCreationRequestJpaEntity(UUID.randomUUID(), companyId,
                actor, key, payloadHash, employeeId, recordedAt));
        return new CreationReceipt(employeeId, episodeId, payloadHash, facts);
    }
}

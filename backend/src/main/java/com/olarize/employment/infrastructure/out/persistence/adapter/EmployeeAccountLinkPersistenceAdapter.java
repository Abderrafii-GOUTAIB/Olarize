package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.application.exception.EmployeeAccountLinkConflict;
import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase.Result;
import com.olarize.employment.application.port.out.EmployeeAccountLinkPort;
import com.olarize.employment.domain.model.EmployeeAccountLinkAction;
import com.olarize.employment.domain.model.EmployeeAccountLinkChange;
import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkAuditJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.mapper.EmployeeAccountLinkPersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeAccountLinkJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeAccountLinkAuditJpaRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class EmployeeAccountLinkPersistenceAdapter implements EmployeeAccountLinkPort {
    private final EmployeeAccountLinkJpaRepository links;
    private final EmployeeAccountLinkAuditJpaRepository audits;
    private final JdbcTemplate database;
    EmployeeAccountLinkPersistenceAdapter(EmployeeAccountLinkJpaRepository links,
            EmployeeAccountLinkAuditJpaRepository audits, JdbcTemplate database) {
        this.links = links; this.audits = audits; this.database = database;
    }
    @Override public Optional<Result> find(UUID companyId, UUID membershipId) {
        return links.findByCompanyIdAndMembershipId(companyId, membershipId).map(EmployeeAccountLinkPersistenceMapper::result);
    }
    @Override public void serialize(UUID companyId, String actor, UUID key) {
        // Narrow PostgreSQL transaction lock; link/audit CRUD remains JPA.
        database.queryForObject("select pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                companyId + ":" + actor + ":EMPLOYEE_LINK:" + key);
    }
    @Override public Optional<Receipt> retry(UUID companyId, String actor, UUID key) {
        return audits.findByCompanyIdAndActorSubjectAndIdempotencyKey(companyId, actor, key)
                .map(a -> new Receipt(a.getPayloadHash(), EmployeeAccountLinkPersistenceMapper.result(a)));
    }
    @Override public boolean employeeExists(UUID companyId, UUID employeeId) {
        // Existence-only security boundary avoids granting administrators HR-record reads.
        return Boolean.TRUE.equals(database.queryForObject("select public.olarize_employee_link_target_exists(?, ?)",
                Boolean.class, companyId, employeeId));
    }
    @Override public Result mutate(UUID companyId, UUID membershipId, EmployeeAccountLinkChange change,
            String actor, UUID key, String hash, String correlationId, Instant recordedAt) {
        var current = links.findByCompanyIdAndMembershipId(companyId, membershipId).orElse(null);
        long version = current == null ? 0 : current.getVersion();
        var previousStatus = current == null ? EmployeeAccountLinkStatus.UNLINKED : current.getStatus();
        UUID previousEmployee = current == null ? null : current.getEmployeeId();
        if (version != change.expectedVersion() || !change.permits(previousStatus, previousEmployee)) {
            throw new EmployeeAccountLinkConflict();
        }
        var nextStatus = change.action() == EmployeeAccountLinkAction.REVOKE ? EmployeeAccountLinkStatus.REVOKED : EmployeeAccountLinkStatus.LINKED;
        if (current == null) current = new EmployeeAccountLinkJpaEntity(companyId, membershipId, change.employeeId(), recordedAt);
        else current.change(change.employeeId(), nextStatus, recordedAt);
        var saved = links.saveAndFlush(current);
        audits.saveAndFlush(new EmployeeAccountLinkAuditJpaEntity(UUID.randomUUID(), companyId, membershipId,
                saved.getEmployeeId(), previousEmployee, saved.getStatus(), previousStatus, saved.getVersion(),
                change.expectedVersion(), change.expectedMembershipVersion(), change.action(), actor, change.reason(),
                change.verificationEvidenceReference(), correlationId, key, hash, recordedAt));
        return EmployeeAccountLinkPersistenceMapper.result(saved);
    }
}

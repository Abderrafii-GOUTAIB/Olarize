package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_creation_audit")
class EmployeeCreationAuditJpaEntity {
    @Id UUID id;
    @Column(name = "employee_id", nullable = false) UUID employeeId;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "actor_subject", nullable = false) String actorSubject;
    @Column(nullable = false) String operation;
    @Column(name = "result_version", nullable = false) long resultVersion;
    @Column(name = "correlation_id") String correlationId;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected EmployeeCreationAuditJpaEntity() { }

    EmployeeCreationAuditJpaEntity(UUID id, UUID employeeId, UUID companyId,
            String actorSubject, String correlationId, Instant recordedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.actorSubject = actorSubject;
        this.operation = "CREATE";
        this.resultVersion = 1;
        this.correlationId = correlationId;
        this.recordedAt = recordedAt;
    }
}

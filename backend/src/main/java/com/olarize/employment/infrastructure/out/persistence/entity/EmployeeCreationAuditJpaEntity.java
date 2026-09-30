package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_creation_audit")
public class EmployeeCreationAuditJpaEntity {
    @Id private UUID id;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "actor_subject", nullable = false) private String actorSubject;
    @Column(nullable = false) private String operation;
    @Column(name = "result_version", nullable = false) private long resultVersion;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected EmployeeCreationAuditJpaEntity() { }

    public EmployeeCreationAuditJpaEntity(UUID id, UUID employeeId, UUID companyId,
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

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getCompanyId() { return companyId; }
    public String getActorSubject() { return actorSubject; }
    public String getOperation() { return operation; }
    public long getResultVersion() { return resultVersion; }
    public String getCorrelationId() { return correlationId; }
    public Instant getRecordedAt() { return recordedAt; }
}

package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_creation_request")
public class EmployeeCreationRequestJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "actor_subject", nullable = false) private String actorSubject;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "payload_hash", nullable = false) private String payloadHash;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected EmployeeCreationRequestJpaEntity() { }

    public EmployeeCreationRequestJpaEntity(UUID id, UUID companyId, String actorSubject,
            UUID idempotencyKey, String payloadHash, UUID employeeId, Instant recordedAt) {
        this.id = id;
        this.companyId = companyId;
        this.actorSubject = actorSubject;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.employeeId = employeeId;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getActorSubject() { return actorSubject; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public UUID getEmployeeId() { return employeeId; }
    public Instant getRecordedAt() { return recordedAt; }
}

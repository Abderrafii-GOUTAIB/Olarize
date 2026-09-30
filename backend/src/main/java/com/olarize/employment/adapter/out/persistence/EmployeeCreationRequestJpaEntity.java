package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_creation_request")
class EmployeeCreationRequestJpaEntity {
    @Id UUID id;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "actor_subject", nullable = false) String actorSubject;
    @Column(name = "idempotency_key", nullable = false) UUID idempotencyKey;
    @Column(name = "payload_hash", nullable = false) String payloadHash;
    @Column(name = "employee_id", nullable = false) UUID employeeId;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected EmployeeCreationRequestJpaEntity() { }

    EmployeeCreationRequestJpaEntity(UUID id, UUID companyId, String actorSubject,
            UUID idempotencyKey, String payloadHash, UUID employeeId, Instant recordedAt) {
        this.id = id;
        this.companyId = companyId;
        this.actorSubject = actorSubject;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.employeeId = employeeId;
        this.recordedAt = recordedAt;
    }
}

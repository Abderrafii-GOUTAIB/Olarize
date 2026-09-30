package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_request")
class ContractMutationRequestJpaEntity {
    @Id UUID id;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "actor_subject", nullable = false) String actorSubject;
    @Column(name = "operation", nullable = false, length = 16) String operation;
    @Column(name = "idempotency_key", nullable = false) UUID idempotencyKey;
    @Column(name = "payload_hash", nullable = false, length = 64) String payloadHash;
    @Column(name = "contract_id", nullable = false) UUID contractId;
    @Column(name = "result_version", nullable = false) long resultVersion;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected ContractMutationRequestJpaEntity() { }

    ContractMutationRequestJpaEntity(UUID id, UUID companyId, String actorSubject,
            String operation, UUID idempotencyKey, String payloadHash,
            UUID contractId, long resultVersion, Instant recordedAt) {
        this.id = id;
        this.companyId = companyId;
        this.actorSubject = actorSubject;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.contractId = contractId;
        this.resultVersion = resultVersion;
        this.recordedAt = recordedAt;
    }
}

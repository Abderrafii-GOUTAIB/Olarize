package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_request")
public class ContractMutationRequestJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "actor_subject", nullable = false) private String actorSubject;
    @Column(name = "operation", nullable = false, length = 16) private String operation;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "contract_id", nullable = false) private UUID contractId;
    @Column(name = "result_version", nullable = false) private long resultVersion;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected ContractMutationRequestJpaEntity() { }

    public ContractMutationRequestJpaEntity(UUID id, UUID companyId, String actorSubject,
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

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getActorSubject() { return actorSubject; }
    public String getOperation() { return operation; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public UUID getContractId() { return contractId; }
    public long getResultVersion() { return resultVersion; }
    public Instant getRecordedAt() { return recordedAt; }
}

package com.olarize.employment.adapter.out.persistence;

import com.olarize.employment.domain.model.ContractReviewState;
import com.olarize.employment.domain.model.ContractType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_version")
class EmploymentContractVersionJpaEntity {
    @Id UUID id;
    @Column(name = "contract_id", nullable = false) UUID contractId;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "employee_id", nullable = false) UUID employeeId;
    @Column(name = "episode_id", nullable = false) UUID episodeId;
    @Column(nullable = false) long version;
    @Enumerated(EnumType.STRING) @Column(name = "type_code", nullable = false, length = 32) ContractType typeCode;
    @Column(name = "start_date", nullable = false) LocalDate startDate;
    @Column(name = "end_date") LocalDate endDate;
    @Column(name = "source_reference", nullable = false, length = 200) String sourceReference;
    @Enumerated(EnumType.STRING) @Column(name = "review_state", nullable = false, length = 32) ContractReviewState reviewState;
    @Column(name = "actor_subject", nullable = false) String actorSubject;
    @Column(name = "operation", nullable = false, length = 16) String operation;
    @Column(name = "change_reason", length = 1000) String changeReason;
    @Column(name = "evidence_reference", length = 200) String evidenceReference;
    @Column(name = "correlation_id", length = 64) String correlationId;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected EmploymentContractVersionJpaEntity() { }

    EmploymentContractVersionJpaEntity(UUID id, UUID contractId, UUID companyId,
            UUID employeeId, UUID episodeId, long version, ContractType typeCode,
            LocalDate startDate, LocalDate endDate, String sourceReference,
            String actorSubject, String operation, String changeReason,
            String evidenceReference, String correlationId, Instant recordedAt) {
        this.id = id;
        this.contractId = contractId;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.episodeId = episodeId;
        this.version = version;
        this.typeCode = typeCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sourceReference = sourceReference;
        this.reviewState = ContractReviewState.UNREVIEWED;
        this.actorSubject = actorSubject;
        this.operation = operation;
        this.changeReason = changeReason;
        this.evidenceReference = evidenceReference;
        this.correlationId = correlationId;
        this.recordedAt = recordedAt;
    }
}

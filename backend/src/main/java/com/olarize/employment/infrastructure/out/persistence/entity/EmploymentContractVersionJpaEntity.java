package com.olarize.employment.infrastructure.out.persistence.entity;

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
public class EmploymentContractVersionJpaEntity {
    @Id private UUID id;
    @Column(name = "contract_id", nullable = false) private UUID contractId;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "episode_id", nullable = false) private UUID episodeId;
    @Column(nullable = false) private long version;
    @Enumerated(EnumType.STRING) @Column(name = "type_code", nullable = false, length = 32) private ContractType typeCode;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "source_reference", nullable = false, length = 200) private String sourceReference;
    @Enumerated(EnumType.STRING) @Column(name = "review_state", nullable = false, length = 32) private ContractReviewState reviewState;
    @Column(name = "actor_subject", nullable = false) private String actorSubject;
    @Column(name = "operation", nullable = false, length = 16) private String operation;
    @Column(name = "change_reason", length = 1000) private String changeReason;
    @Column(name = "evidence_reference", length = 200) private String evidenceReference;
    @Column(name = "correlation_id", length = 64) private String correlationId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected EmploymentContractVersionJpaEntity() { }

    public EmploymentContractVersionJpaEntity(UUID id, UUID contractId, UUID companyId,
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

    public UUID getId() { return id; }
    public UUID getContractId() { return contractId; }
    public UUID getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getEpisodeId() { return episodeId; }
    public long getVersion() { return version; }
    public ContractType getTypeCode() { return typeCode; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getSourceReference() { return sourceReference; }
    public ContractReviewState getReviewState() { return reviewState; }
    public String getActorSubject() { return actorSubject; }
    public String getOperation() { return operation; }
    public String getChangeReason() { return changeReason; }
    public String getEvidenceReference() { return evidenceReference; }
    public String getCorrelationId() { return correlationId; }
    public Instant getRecordedAt() { return recordedAt; }
}

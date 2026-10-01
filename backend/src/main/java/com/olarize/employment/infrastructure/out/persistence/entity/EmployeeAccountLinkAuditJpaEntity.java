package com.olarize.employment.infrastructure.out.persistence.entity;

import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import com.olarize.employment.domain.model.EmployeeAccountLinkAction;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_account_link_audit")
public class EmployeeAccountLinkAuditJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "membership_id") private UUID membershipId;
    @Column(name = "employee_id") private UUID employeeId;
    @Column(name = "previous_employee_id") private UUID previousEmployeeId;
    @Enumerated(EnumType.STRING) private EmployeeAccountLinkStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status") private EmployeeAccountLinkStatus previousStatus;
    private long version;
    @Column(name = "expected_version") private long expectedVersion;
    @Column(name = "membership_version") private long membershipVersion;
    @Enumerated(EnumType.STRING) private EmployeeAccountLinkAction action;
    @Column(name = "actor_subject") private String actorSubject;
    private String reason;
    @Column(name = "verification_evidence_reference") private String verificationEvidenceReference;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "idempotency_key") private UUID idempotencyKey;
    @Column(name = "payload_hash") private String payloadHash;
    @Column(name = "recorded_at") private Instant recordedAt;

    protected EmployeeAccountLinkAuditJpaEntity() { }
    public EmployeeAccountLinkAuditJpaEntity(UUID id, UUID companyId, UUID membershipId, UUID employeeId, UUID previousEmployeeId, EmployeeAccountLinkStatus status, EmployeeAccountLinkStatus previousStatus, long version, long expectedVersion, long membershipVersion, EmployeeAccountLinkAction action, String actorSubject, String reason, String verificationEvidenceReference, String correlationId, UUID idempotencyKey, String payloadHash, Instant recordedAt) {
        this.id = id;
        this.companyId = companyId;
        this.membershipId = membershipId;
        this.employeeId = employeeId;
        this.previousEmployeeId = previousEmployeeId;
        this.status = status;
        this.previousStatus = previousStatus;
        this.version = version;
        this.expectedVersion = expectedVersion;
        this.membershipVersion = membershipVersion;
        this.action = action;
        this.actorSubject = actorSubject;
        this.reason = reason;
        this.verificationEvidenceReference = verificationEvidenceReference;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.recordedAt = recordedAt;
    }
    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getMembershipId() { return membershipId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getPreviousEmployeeId() { return previousEmployeeId; }
    public EmployeeAccountLinkStatus getStatus() { return status; }
    public EmployeeAccountLinkStatus getPreviousStatus() { return previousStatus; }
    public long getVersion() { return version; }
    public long getExpectedVersion() { return expectedVersion; }
    public long getMembershipVersion() { return membershipVersion; }
    public EmployeeAccountLinkAction getAction() { return action; }
    public String getActorSubject() { return actorSubject; }
    public String getReason() { return reason; }
    public String getVerificationEvidenceReference() { return verificationEvidenceReference; }
    public String getCorrelationId() { return correlationId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getRecordedAt() { return recordedAt; }
}

package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Immutable source snapshot and restricted audit record; runtime has no UPDATE/DELETE grants. */
@Entity
@Table(name = "employment_employee_version")
public class EmployeeVersionJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "employee_id") private UUID employeeId;
    @Column(name = "episode_id") private UUID episodeId;
    private long version;
    @Column(name = "episode_version") private long episodeVersion;
    private String reference;
    @Column(name = "normalized_reference") private String normalizedReference;
    private String name;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "job_title") private String jobTitle;
    private String department;
    private String site;
    @Column(name = "work_email") private String workEmail;
    @Column(name = "work_phone") private String workPhone;
    @Column(name = "effective_date") private LocalDate effectiveDate;
    @Column(name = "job_changed") private boolean jobChanged;
    @Column(name = "actor_subject") private String actorSubject;
    private String reason;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "recorded_at") private Instant recordedAt;
    @Column(name = "idempotency_key") private UUID idempotencyKey;
    @Column(name = "payload_hash") private String payloadHash;

    protected EmployeeVersionJpaEntity() { }

    public EmployeeVersionJpaEntity(UUID id, UUID companyId, UUID employeeId, UUID episodeId, long version, long episodeVersion, String reference, String normalizedReference, String name, LocalDate startDate, String jobTitle, String department, String site, String workEmail, String workPhone, LocalDate effectiveDate, boolean jobChanged, String actorSubject, String reason, String correlationId, Instant recordedAt, UUID idempotencyKey, String payloadHash) {
        this.id = id;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.episodeId = episodeId;
        this.version = version;
        this.episodeVersion = episodeVersion;
        this.reference = reference;
        this.normalizedReference = normalizedReference;
        this.name = name;
        this.startDate = startDate;
        this.jobTitle = jobTitle;
        this.department = department;
        this.site = site;
        this.workEmail = workEmail;
        this.workPhone = workPhone;
        this.effectiveDate = effectiveDate;
        this.jobChanged = jobChanged;
        this.actorSubject = actorSubject;
        this.reason = reason;
        this.correlationId = correlationId;
        this.recordedAt = recordedAt;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getEpisodeId() { return episodeId; }
    public long getVersion() { return version; }
    public long getEpisodeVersion() { return episodeVersion; }
    public String getReference() { return reference; }
    public String getNormalizedReference() { return normalizedReference; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getSite() { return site; }
    public String getWorkEmail() { return workEmail; }
    public String getWorkPhone() { return workPhone; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public boolean getJobChanged() { return jobChanged; }
    public String getActorSubject() { return actorSubject; }
    public String getReason() { return reason; }
    public String getCorrelationId() { return correlationId; }
    public Instant getRecordedAt() { return recordedAt; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
}

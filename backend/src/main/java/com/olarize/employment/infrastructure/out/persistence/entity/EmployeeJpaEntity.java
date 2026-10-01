package com.olarize.employment.infrastructure.out.persistence.entity;

import com.olarize.employment.domain.model.EmployeeCreation;
import jakarta.persistence.Version;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@DynamicUpdate
@Table(name = "employment_employee")
public class EmployeeJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(nullable = false) private String reference;
    @Column(name = "normalized_reference", nullable = false) private String normalizedReference;
    @Column(nullable = false) private String name;
    @Column(name = "job_title") private String jobTitle;
    private String department;
    private String site;
    @Column(name = "work_email") private String workEmail;
    @Column(name = "work_phone") private String workPhone;
    @Version private long version;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected EmployeeJpaEntity() { }

    public EmployeeJpaEntity(UUID id, UUID companyId, String reference, String normalizedReference,
            String name, String jobTitle, String department, String site,
            String workEmail, String workPhone, Instant recordedAt) {
        this.id = id;
        this.companyId = companyId;
        this.reference = reference;
        this.normalizedReference = normalizedReference;
        this.name = name;
        this.jobTitle = jobTitle;
        this.department = department;
        this.site = site;
        this.workEmail = workEmail;
        this.workPhone = workPhone;
        this.version = 1;
        this.recordedAt = recordedAt;
    }

    public void correct(EmployeeCreation facts, Instant time) {
        reference = facts.reference();
        normalizedReference = facts.normalizedReference();
        name = facts.name();
        workEmail = facts.workEmail();
        workPhone = facts.workPhone();
        recordedAt = time;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getReference() { return reference; }
    public String getNormalizedReference() { return normalizedReference; }
    public String getName() { return name; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getSite() { return site; }
    public String getWorkEmail() { return workEmail; }
    public String getWorkPhone() { return workPhone; }
    public long getVersion() { return version; }
    public Instant getRecordedAt() { return recordedAt; }
}

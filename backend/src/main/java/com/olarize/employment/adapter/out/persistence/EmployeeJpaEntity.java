package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_employee")
class EmployeeJpaEntity {
    @Id UUID id;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(nullable = false) String reference;
    @Column(name = "normalized_reference", nullable = false) String normalizedReference;
    @Column(nullable = false) String name;
    @Column(name = "job_title") String jobTitle;
    String department;
    String site;
    @Column(name = "work_email") String workEmail;
    @Column(name = "work_phone") String workPhone;
    long version;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected EmployeeJpaEntity() { }

    EmployeeJpaEntity(UUID id, UUID companyId, String reference, String normalizedReference,
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
}

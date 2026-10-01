package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employment_job_version")
public class EmployeeJobJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "episode_id") private UUID episodeId;
    @Column(name = "employee_id") private UUID employeeId;
    @Column(name = "source_version") private long sourceVersion;
    @Column(name = "effective_date") private LocalDate effectiveDate;
    @Column(name = "job_title") private String jobTitle;
    private String department;
    private String site;
    protected EmployeeJobJpaEntity() { }
    public EmployeeJobJpaEntity(UUID companyId, UUID employeeId, UUID episodeId, long sourceVersion, LocalDate effectiveDate,
            String jobTitle, String department, String site) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.episodeId = episodeId;
        this.employeeId = employeeId;
        this.sourceVersion = sourceVersion;
        this.effectiveDate = effectiveDate;
        this.jobTitle = jobTitle;
        this.department = department;
        this.site = site;
    }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getSite() { return site; }
}

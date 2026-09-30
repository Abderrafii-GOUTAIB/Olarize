package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "employment_contract")
public class EmploymentContractJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "episode_id", nullable = false) private UUID episodeId;
    @Column(name = "current_version", nullable = false) private long currentVersion;
    @Version @Column(name = "lock_version", nullable = false) private Long lockVersion;

    protected EmploymentContractJpaEntity() { }

    public EmploymentContractJpaEntity(UUID id, UUID companyId, UUID employeeId, UUID episodeId) {
        this.id = id;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.episodeId = episodeId;
        this.currentVersion = 1;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getEpisodeId() { return episodeId; }
    public long getCurrentVersion() { return currentVersion; }
    public Long getLockVersion() { return lockVersion; }
    public void setCurrentVersion(long currentVersion) { this.currentVersion = currentVersion; }
}

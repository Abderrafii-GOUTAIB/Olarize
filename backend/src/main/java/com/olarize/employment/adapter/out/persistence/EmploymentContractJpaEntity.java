package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "employment_contract")
class EmploymentContractJpaEntity {
    @Id UUID id;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "employee_id", nullable = false) UUID employeeId;
    @Column(name = "episode_id", nullable = false) UUID episodeId;
    @Column(name = "current_version", nullable = false) long currentVersion;
    @Version @Column(name = "lock_version", nullable = false) Long lockVersion;

    protected EmploymentContractJpaEntity() { }

    EmploymentContractJpaEntity(UUID id, UUID companyId, UUID employeeId, UUID episodeId) {
        this.id = id;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.episodeId = episodeId;
        this.currentVersion = 1;
    }
}

package com.olarize.employment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employment_episode")
class EmploymentEpisodeJpaEntity {
    @Id UUID id;
    @Column(name = "employee_id", nullable = false) UUID employeeId;
    @Column(name = "company_id", nullable = false) UUID companyId;
    @Column(name = "start_date", nullable = false) LocalDate startDate;
    @Column(name = "last_employed_day") LocalDate lastEmployedDay;
    long version;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;

    protected EmploymentEpisodeJpaEntity() { }

    EmploymentEpisodeJpaEntity(UUID id, UUID employeeId, UUID companyId, LocalDate startDate, Instant recordedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.startDate = startDate;
        this.version = 1;
        this.recordedAt = recordedAt;
    }
}

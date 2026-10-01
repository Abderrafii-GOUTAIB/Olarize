package com.olarize.employment.infrastructure.out.persistence.entity;

import jakarta.persistence.Version;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@DynamicUpdate
@Table(name = "employment_episode")
public class EmploymentEpisodeJpaEntity {
    @Id private UUID id;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "last_employed_day") private LocalDate lastEmployedDay;
    @Version private long version;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    protected EmploymentEpisodeJpaEntity() { }

    public EmploymentEpisodeJpaEntity(UUID id, UUID employeeId, UUID companyId, LocalDate startDate, Instant recordedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.startDate = startDate;
        this.version = 1;
        this.recordedAt = recordedAt;
    }

    public void correctStart(LocalDate startDate, Instant time) {
        this.startDate = startDate;
        this.recordedAt = time;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getCompanyId() { return companyId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getLastEmployedDay() { return lastEmployedDay; }
    public long getVersion() { return version; }
    public Instant getRecordedAt() { return recordedAt; }
}

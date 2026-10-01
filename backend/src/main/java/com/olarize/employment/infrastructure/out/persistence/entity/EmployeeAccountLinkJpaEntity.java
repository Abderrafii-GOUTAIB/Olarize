package com.olarize.employment.infrastructure.out.persistence.entity;

import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_account_link")
public class EmployeeAccountLinkJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Enumerated(EnumType.STRING) private EmployeeAccountLinkStatus status;
    @Version private long version;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;
    protected EmployeeAccountLinkJpaEntity() { }
    public EmployeeAccountLinkJpaEntity(UUID companyId, UUID membershipId, UUID employeeId, Instant time) {
        this.id = UUID.randomUUID(); this.companyId = companyId; this.membershipId = membershipId;
        this.employeeId = employeeId; this.status = EmployeeAccountLinkStatus.LINKED;
        this.version = 1; this.recordedAt = time;
    }
    public void change(UUID employeeId, EmployeeAccountLinkStatus status, Instant time) {
        if (employeeId != null) this.employeeId = employeeId;
        this.status = status; this.recordedAt = time;
    }
    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getMembershipId() { return membershipId; }
    public UUID getEmployeeId() { return employeeId; }
    public EmployeeAccountLinkStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getRecordedAt() { return recordedAt; }
}

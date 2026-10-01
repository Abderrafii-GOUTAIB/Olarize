package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkAuditJpaEntity;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeAccountLinkAuditJpaRepository extends JpaRepository<EmployeeAccountLinkAuditJpaEntity, UUID> {
    Optional<EmployeeAccountLinkAuditJpaEntity> findByCompanyIdAndActorSubjectAndIdempotencyKey(UUID companyId, String actor, UUID key);
}

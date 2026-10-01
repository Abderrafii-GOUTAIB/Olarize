package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeVersionJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeVersionJpaRepository extends JpaRepository<EmployeeVersionJpaEntity, UUID> {
    Optional<EmployeeVersionJpaEntity> findByCompanyIdAndActorSubjectAndIdempotencyKey(UUID companyId, String actorSubject, UUID key);
    Optional<EmployeeVersionJpaEntity> findByCompanyIdAndEmployeeIdAndVersion(UUID companyId, UUID employeeId, long version);
}

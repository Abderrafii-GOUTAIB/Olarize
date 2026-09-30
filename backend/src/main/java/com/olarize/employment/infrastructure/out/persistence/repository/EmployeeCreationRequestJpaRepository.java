package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeCreationRequestJpaEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeCreationRequestJpaRepository extends JpaRepository<EmployeeCreationRequestJpaEntity, UUID> {
    Optional<EmployeeCreationRequestJpaEntity> findByCompanyIdAndActorSubjectAndIdempotencyKey(
            UUID companyId, String actorSubject, UUID idempotencyKey);
}

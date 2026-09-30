package com.olarize.employment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeCreationRequestJpaRepository extends JpaRepository<EmployeeCreationRequestJpaEntity, UUID> {
    Optional<EmployeeCreationRequestJpaEntity> findByCompanyIdAndActorSubjectAndIdempotencyKey(
            UUID companyId, String actorSubject, UUID idempotencyKey);
}

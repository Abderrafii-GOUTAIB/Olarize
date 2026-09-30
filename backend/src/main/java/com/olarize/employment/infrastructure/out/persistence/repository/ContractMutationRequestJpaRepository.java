package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.ContractMutationRequestJpaEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractMutationRequestJpaRepository extends JpaRepository<ContractMutationRequestJpaEntity, UUID> {
    Optional<ContractMutationRequestJpaEntity> findByCompanyIdAndActorSubjectAndOperationAndIdempotencyKey(
            UUID companyId, String actorSubject, String operation, UUID idempotencyKey);
}

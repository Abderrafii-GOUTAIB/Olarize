package com.olarize.employment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContractMutationRequestJpaRepository extends JpaRepository<ContractMutationRequestJpaEntity, UUID> {
    Optional<ContractMutationRequestJpaEntity> findByCompanyIdAndActorSubjectAndOperationAndIdempotencyKey(
            UUID companyId, String actorSubject, String operation, UUID idempotencyKey);
}

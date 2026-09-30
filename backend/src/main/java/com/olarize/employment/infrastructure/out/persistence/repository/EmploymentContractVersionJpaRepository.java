package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentContractVersionJpaEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentContractVersionJpaRepository extends JpaRepository<EmploymentContractVersionJpaEntity, UUID> {
    Optional<EmploymentContractVersionJpaEntity> findByContractIdAndCompanyIdAndVersion(
            UUID contractId, UUID companyId, long version);
}

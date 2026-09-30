package com.olarize.employment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmploymentContractVersionJpaRepository extends JpaRepository<EmploymentContractVersionJpaEntity, UUID> {
    Optional<EmploymentContractVersionJpaEntity> findByContractIdAndCompanyIdAndVersion(
            UUID contractId, UUID companyId, long version);
}

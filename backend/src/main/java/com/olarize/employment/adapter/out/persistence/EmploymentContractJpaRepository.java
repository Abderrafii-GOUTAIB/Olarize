package com.olarize.employment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmploymentContractJpaRepository extends JpaRepository<EmploymentContractJpaEntity, UUID> {
    Optional<EmploymentContractJpaEntity> findByIdAndCompanyIdAndEmployeeIdAndEpisodeId(
            UUID id, UUID companyId, UUID employeeId, UUID episodeId);
}

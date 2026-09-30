package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentContractJpaEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentContractJpaRepository extends JpaRepository<EmploymentContractJpaEntity, UUID> {
    Optional<EmploymentContractJpaEntity> findByIdAndCompanyIdAndEmployeeIdAndEpisodeId(
            UUID id, UUID companyId, UUID employeeId, UUID episodeId);
}

package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentEpisodeJpaEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentEpisodeJpaRepository extends JpaRepository<EmploymentEpisodeJpaEntity, UUID> {
    Optional<EmploymentEpisodeJpaEntity> findByEmployeeIdAndCompanyId(UUID employeeId, UUID companyId);
}

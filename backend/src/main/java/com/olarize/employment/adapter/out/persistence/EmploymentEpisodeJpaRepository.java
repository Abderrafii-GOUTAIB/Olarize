package com.olarize.employment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmploymentEpisodeJpaRepository extends JpaRepository<EmploymentEpisodeJpaEntity, UUID> {
    Optional<EmploymentEpisodeJpaEntity> findByEmployeeIdAndCompanyId(UUID employeeId, UUID companyId);
}

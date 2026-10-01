package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkJpaEntity;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeAccountLinkJpaRepository extends JpaRepository<EmployeeAccountLinkJpaEntity, UUID> {
    Optional<EmployeeAccountLinkJpaEntity> findByCompanyIdAndMembershipId(UUID companyId, UUID membershipId);
}

package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeCreationAuditJpaEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeCreationAuditJpaRepository extends JpaRepository<EmployeeCreationAuditJpaEntity, UUID> { }

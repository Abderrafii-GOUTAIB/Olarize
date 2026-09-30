package com.olarize.employment.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeCreationAuditJpaRepository extends JpaRepository<EmployeeCreationAuditJpaEntity, UUID> { }

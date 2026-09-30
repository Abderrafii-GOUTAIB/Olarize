package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.infrastructure.out.persistence.repository.ExpiringContractsJpaRepository;

import com.olarize.employment.application.port.in.ExpiringContract;

public final class ExpiringContractsPersistenceMapper {
    private ExpiringContractsPersistenceMapper() { }

    public static ExpiringContract toModel(ExpiringContractsJpaRepository.ExpiringContractProjection row) {
        return new ExpiringContract(row.getContractId(), row.getContractVersion(), row.getEmployeeId(),
                row.getEmployeeName(), row.getEmployeeReference(), row.getStartDate(), row.getEndDate());
    }
}

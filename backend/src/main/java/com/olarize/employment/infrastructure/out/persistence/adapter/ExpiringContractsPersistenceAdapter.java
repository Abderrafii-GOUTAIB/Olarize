package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.infrastructure.out.persistence.mapper.ExpiringContractsPersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.repository.ExpiringContractsJpaRepository;

import com.olarize.employment.application.port.in.ExpiringContractsPage;
import com.olarize.employment.application.port.out.ExpiringContractsPort;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class ExpiringContractsPersistenceAdapter implements ExpiringContractsPort {
    private final ExpiringContractsJpaRepository contracts;

    ExpiringContractsPersistenceAdapter(ExpiringContractsJpaRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public ExpiringContractsPage findCurrentVersions(UUID companyId, LocalDate fromDate,
            LocalDate toDate, int page, int pageSize) {
        var result = contracts.findCurrentVersions(companyId, fromDate, toDate, PageRequest.of(page, pageSize));
        return new ExpiringContractsPage(result.map(ExpiringContractsPersistenceMapper::toModel).getContent(),
                result.getTotalElements());
    }

    @Override
    public long countCurrentContractsWithoutEndDate(UUID companyId) {
        return contracts.countCurrentContractsWithoutEndDate(companyId);
    }
}

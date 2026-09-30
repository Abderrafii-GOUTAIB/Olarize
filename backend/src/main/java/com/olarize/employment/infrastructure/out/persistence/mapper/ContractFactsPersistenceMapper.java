package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentContractVersionJpaEntity;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import java.time.temporal.ChronoUnit;

public final class ContractFactsPersistenceMapper {
    private ContractFactsPersistenceMapper() { }

    public static ContractVersionResponse response(EmploymentContractVersionJpaEntity version) {
        return new ContractVersionResponse(version.getContractId(), version.getCompanyId(),
                version.getEmployeeId(), version.getEpisodeId(), version.getVersion(), version.getTypeCode(),
                version.getStartDate(), version.getEndDate(), version.getSourceReference(), version.getReviewState(),
                version.getChangeReason(), version.getEvidenceReference(),
                version.getRecordedAt().plusNanos(500).truncatedTo(ChronoUnit.MICROS));
    }
}

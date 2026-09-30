package com.olarize.employment.adapter.out.persistence;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import java.time.temporal.ChronoUnit;

final class ContractFactsPersistenceMapper {
    private ContractFactsPersistenceMapper() { }

    static ContractVersionResponse response(EmploymentContractVersionJpaEntity version) {
        return new ContractVersionResponse(version.contractId, version.companyId,
                version.employeeId, version.episodeId, version.version, version.typeCode,
                version.startDate, version.endDate, version.sourceReference, version.reviewState,
                version.changeReason, version.evidenceReference,
                version.recordedAt.plusNanos(500).truncatedTo(ChronoUnit.MICROS));
    }
}

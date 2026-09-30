package com.olarize.employment.adapter.in.web;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.domain.model.ContractFacts;

final class ContractWebMapper {
    private ContractWebMapper() { }

    static ContractFacts facts(CreateContractRequest request) {
        return ContractFacts.of(request.typeCode(), request.startDate(), request.endDate(), request.sourceReference());
    }

    static ContractFacts facts(CorrectContractRequest request) {
        return ContractFacts.of(request.typeCode(), request.startDate(), request.endDate(), request.sourceReference());
    }

    static ContractResponse response(ContractVersionResponse version) {
        return new ContractResponse(version.contractId(), version.companyId(), version.employeeId(),
                version.episodeId(), version.version(), version.typeCode(), version.startDate(),
                version.endDate(), version.endDate() == null ? "NOT_RECORDED" : "RECORDED",
                version.sourceReference(), version.reviewState(), version.changeReason(),
                version.evidenceReference(), version.recordedAt());
    }
}

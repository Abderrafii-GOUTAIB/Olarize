package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.infrastructure.in.web.dto.request.CorrectContractRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.request.CreateContractRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.ContractResponseDto;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.domain.model.ContractFacts;

public final class ContractWebMapper {
    private ContractWebMapper() { }

    public static ContractFacts facts(CreateContractRequestDto request) {
        return ContractFacts.of(request.typeCode(), request.startDate(), request.endDate(), request.sourceReference());
    }

    public static ContractFacts facts(CorrectContractRequestDto request) {
        return ContractFacts.of(request.typeCode(), request.startDate(), request.endDate(), request.sourceReference());
    }

    public static ContractResponseDto response(ContractVersionResponse version) {
        return new ContractResponseDto(version.contractId(), version.companyId(), version.employeeId(),
                version.episodeId(), version.version(), version.typeCode(), version.startDate(),
                version.endDate(), version.endDate() == null ? "NOT_RECORDED" : "RECORDED",
                version.sourceReference(), version.reviewState(), version.changeReason(),
                version.evidenceReference(), version.recordedAt());
    }
}

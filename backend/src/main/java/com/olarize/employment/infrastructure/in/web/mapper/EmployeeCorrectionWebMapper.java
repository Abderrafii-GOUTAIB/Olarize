package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.application.port.in.CorrectEmployeeUseCase.Result;
import com.olarize.employment.domain.model.EmployeeCorrection;
import com.olarize.employment.domain.model.EmployeeCreation;
import com.olarize.employment.infrastructure.in.web.dto.request.CorrectEmployeeRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.CorrectedEmployeeResponseDto;

public final class EmployeeCorrectionWebMapper {
    private EmployeeCorrectionWebMapper() { }
    public static EmployeeCorrection correction(CorrectEmployeeRequestDto request) {
        return new EmployeeCorrection(EmployeeCreation.of(request.reference(), request.name(), request.startDate(),
                request.jobTitle(), request.department(), request.site(), request.workEmail(), request.workPhone()),
                request.effectiveDate(), request.reason());
    }
    public static CorrectedEmployeeResponseDto response(Result result) {
        return new CorrectedEmployeeResponseDto(result.companyId(), result.employeeId(), result.episodeId(),
                result.version(), result.episodeVersion(), result.effectiveDate(), result.recordedAt());
    }
}

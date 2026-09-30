package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.infrastructure.in.web.dto.request.CreateEmployeeRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.CreatedEmployeeResponseDto;

import com.olarize.employment.application.port.in.CreateEmployeeUseCase;
import com.olarize.employment.domain.model.EmployeeCreation;

public final class EmployeeWebMapper {
    private EmployeeWebMapper() { }

    public static EmployeeCreation facts(CreateEmployeeRequestDto request) {
        return EmployeeCreation.of(request.reference(), request.name(), request.startDate(),
                request.jobTitle(), request.department(), request.site(),
                request.workEmail(), request.workPhone());
    }

    public static CreatedEmployeeResponseDto response(CreateEmployeeUseCase.CreatedEmployee created) {
        return new CreatedEmployeeResponseDto(created.employeeId(), created.episodeId(),
                created.companyId(), created.reference(), created.name(), created.startDate(),
                created.status(), created.version());
    }
}

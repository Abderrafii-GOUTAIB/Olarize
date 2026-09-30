package com.olarize.employment.adapter.in.web;

import com.olarize.employment.application.port.in.CreateEmployeeUseCase;
import com.olarize.employment.domain.model.EmployeeCreation;

final class EmployeeWebMapper {
    private EmployeeWebMapper() { }

    static EmployeeCreation facts(CreateEmployeeRequest request) {
        return EmployeeCreation.of(request.reference(), request.name(), request.startDate(),
                request.jobTitle(), request.department(), request.site(),
                request.workEmail(), request.workPhone());
    }

    static CreatedEmployeeResponse response(CreateEmployeeUseCase.CreatedEmployee created) {
        return new CreatedEmployeeResponse(created.employeeId(), created.episodeId(),
                created.companyId(), created.reference(), created.name(), created.startDate(),
                created.status(), created.version());
    }
}

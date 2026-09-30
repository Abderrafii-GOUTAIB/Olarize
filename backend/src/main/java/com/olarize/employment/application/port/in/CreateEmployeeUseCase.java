package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.EmployeeCreation;
import com.olarize.employment.domain.model.EmploymentStatus;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateEmployeeUseCase {
    CreatedEmployee create(UUID companyId, EmployeeCreation facts, UUID idempotencyKey, String correlationId);

    record CreatedEmployee(UUID employeeId, UUID episodeId, UUID companyId, String reference,
            String name, LocalDate startDate, EmploymentStatus status, long version) { }
}

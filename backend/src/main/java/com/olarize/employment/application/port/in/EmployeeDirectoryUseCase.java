package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import java.time.LocalDate;
import java.util.UUID;

public interface EmployeeDirectoryUseCase {
    EmployeeDirectoryResult list(UUID companyId, LocalDate asOf, EmployeeDirectoryFilter filter);
    EmployeeDetailResult detail(UUID companyId, UUID employeeId, LocalDate asOf);
}

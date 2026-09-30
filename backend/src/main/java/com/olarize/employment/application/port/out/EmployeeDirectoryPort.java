package com.olarize.employment.application.port.out;

import com.olarize.employment.application.port.in.EmployeeDirectoryPage;
import com.olarize.employment.application.port.in.EmployeeRecord;
import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDirectoryPort {
    EmployeeDirectoryPage find(UUID companyId, LocalDate asOf, EmployeeDirectoryFilter filter);
    Optional<EmployeeRecord> findById(UUID companyId, UUID employeeId, LocalDate asOf);
}

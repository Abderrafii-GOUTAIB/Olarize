package com.olarize.employment.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDirectoryResult(UUID companyId, LocalDate asOf, String timeZone,
        EmployeeDirectoryPage records, int page, int pageSize, int totalPages) { }

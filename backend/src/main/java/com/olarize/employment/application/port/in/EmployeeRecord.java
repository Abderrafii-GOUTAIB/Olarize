package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.EmploymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRecord(UUID employeeId, UUID companyId, String reference, String name,
        String jobTitle, String department, String site, String workEmail, String workPhone,
        long version, Instant recordedAt, UUID episodeId, LocalDate startDate, LocalDate lastEmployedDay,
        long episodeVersion, EmploymentStatus status) { }

package com.olarize.employment.infrastructure.in.web.dto.response;

import com.olarize.employment.domain.model.EmploymentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeSummaryResponseDto(UUID employeeId, String reference, String name,
        String jobTitle, String department, String site, EmploymentStatus status,
        UUID episodeId, LocalDate startDate, LocalDate lastEmployedDay, long version, long episodeVersion) { }

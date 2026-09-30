package com.olarize.employment.adapter.in.web;

import com.olarize.employment.domain.model.EmploymentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CreatedEmployeeResponse(UUID employeeId, UUID episodeId, UUID companyId,
        String reference, String name, LocalDate startDate, EmploymentStatus status, long version) { }

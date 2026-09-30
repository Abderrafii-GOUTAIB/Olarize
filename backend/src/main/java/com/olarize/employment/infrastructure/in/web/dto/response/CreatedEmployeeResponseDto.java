package com.olarize.employment.infrastructure.in.web.dto.response;

import com.olarize.employment.domain.model.EmploymentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CreatedEmployeeResponseDto(UUID employeeId, UUID episodeId, UUID companyId,
        String reference, String name, LocalDate startDate, EmploymentStatus status, long version) { }

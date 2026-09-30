package com.olarize.employment.infrastructure.in.web.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeDirectoryResponseDto(UUID companyId, LocalDate asOf, String timeZone,
        List<EmployeeSummaryResponseDto> items, EmployeeDirectoryPaginationResponseDto pagination) {
    public EmployeeDirectoryResponseDto { items = List.copyOf(items); }
}

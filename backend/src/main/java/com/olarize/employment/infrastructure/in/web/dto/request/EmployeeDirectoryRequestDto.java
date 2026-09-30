package com.olarize.employment.infrastructure.in.web.dto.request;

import com.olarize.employment.domain.model.EmployeeDirectoryStatus;
import java.time.LocalDate;

public record EmployeeDirectoryRequestDto(String search, EmployeeDirectoryStatus status,
        LocalDate asOf, int page, int pageSize) { }

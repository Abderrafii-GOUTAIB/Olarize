package com.olarize.employment.infrastructure.in.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ExpiringContractResponseDto(UUID contractId, long contractVersion, UUID employeeId,
        String employeeName, String employeeReference, LocalDate startDate, LocalDate endDate) { }

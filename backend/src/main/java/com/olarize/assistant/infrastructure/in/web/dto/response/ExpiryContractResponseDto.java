package com.olarize.assistant.infrastructure.in.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ExpiryContractResponseDto(UUID contractId, long contractVersion, UUID employeeId,
        String employeeName, String employeeReference, LocalDate startDate, LocalDate endDate) { }

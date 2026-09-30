package com.olarize.employment.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public record ExpiringContract(UUID contractId, long contractVersion, UUID employeeId,
        String employeeName, String employeeReference, LocalDate startDate, LocalDate endDate) { }

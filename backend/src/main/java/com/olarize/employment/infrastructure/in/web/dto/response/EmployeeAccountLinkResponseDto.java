package com.olarize.employment.infrastructure.in.web.dto.response;

import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import java.time.Instant;
import java.util.UUID;

public record EmployeeAccountLinkResponseDto(UUID companyId, UUID membershipId, UUID employeeId,
        EmployeeAccountLinkStatus status, long version, Instant recordedAt) { }

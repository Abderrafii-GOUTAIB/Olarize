package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.ContractReviewState;
import com.olarize.employment.domain.model.ContractType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractVersionResponse(UUID contractId, UUID companyId, UUID employeeId,
        UUID episodeId, long version, ContractType typeCode, LocalDate startDate,
        LocalDate endDate, String sourceReference, ContractReviewState reviewState,
        String changeReason, String evidenceReference, Instant recordedAt) { }

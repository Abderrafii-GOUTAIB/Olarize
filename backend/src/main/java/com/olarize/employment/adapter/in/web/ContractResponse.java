package com.olarize.employment.adapter.in.web;

import com.olarize.employment.domain.model.ContractReviewState;
import com.olarize.employment.domain.model.ContractType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(UUID contractId, UUID companyId, UUID employeeId,
        UUID episodeId, long version, ContractType typeCode, LocalDate startDate,
        LocalDate endDate, String endDateCompleteness, String sourceReference,
        ContractReviewState reviewState, String changeReason, String evidenceReference,
        Instant recordedAt) { }

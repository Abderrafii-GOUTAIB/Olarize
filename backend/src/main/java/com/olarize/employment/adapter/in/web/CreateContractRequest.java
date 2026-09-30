package com.olarize.employment.adapter.in.web;

import com.olarize.employment.domain.model.ContractType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateContractRequest(
        @Schema(description = "Contract type code supplied by HR; this is not a legal validity decision", example = "CDD")
        @NotNull ContractType typeCode,
        @Schema(description = "Recorded contract start civil date", example = "2026-09-01")
        @NotNull LocalDate startDate,
        @Schema(description = "Recorded contract end date; null means no end date was supplied", example = "2027-08-31")
        LocalDate endDate,
        @Schema(description = "HR source label or reference; no attachment is uploaded", example = "Paper contract archive 2026-09")
        @NotBlank @Size(max = 200) String sourceReference,
        @Schema(description = "Retry key for this HR actor and company", example = "108f9945-6aae-4f39-8ab5-8b11fdb99772")
        @NotNull UUID idempotencyKey,
        @Schema(description = "HR confirms these exact recorded facts", example = "true")
        @NotNull @AssertTrue Boolean confirm) { }

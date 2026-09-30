package com.olarize.employment.infrastructure.in.web.dto.request;

import com.olarize.employment.domain.model.ContractType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectContractRequestDto(
        @Schema(description = "The version HR reviewed before making this correction", example = "1")
        @NotNull @Min(1) Long expectedVersion,
        @Schema(description = "Corrected HR-recorded contract type code", example = "CDD")
        @NotNull ContractType typeCode,
        @Schema(description = "Corrected contract start date", example = "2026-09-01")
        @NotNull LocalDate startDate,
        @Schema(description = "Corrected end date; null means no end date was supplied", example = "2027-12-31")
        LocalDate endDate,
        @Schema(description = "Updated HR source label or reference", example = "Amended paper contract, page 2")
        @NotBlank @Size(max = 200) String sourceReference,
        @Schema(description = "Why HR is correcting the prior facts", example = "The original end date was transcribed incorrectly.")
        @NotBlank @Size(max = 1000) String reason,
        @Schema(description = "Reference to the evidence HR used; no file is uploaded", example = "HR archive item DOC-2026-0042")
        @NotBlank @Size(max = 200) String evidenceReference,
        @Schema(description = "Retry key for this HR actor and company", example = "894f5ba8-e8f9-459d-921e-51605201fb08")
        @NotNull UUID idempotencyKey,
        @Schema(description = "HR confirms these exact corrected facts", example = "true")
        @NotNull @AssertTrue Boolean confirm) { }

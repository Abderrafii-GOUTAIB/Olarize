package com.olarize.employment.infrastructure.in.web.dto.request;

import com.olarize.employment.domain.model.EmployeeAccountLinkAction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChangeEmployeeAccountLinkRequestDto(
        @NotNull EmployeeAccountLinkAction action,
        UUID employeeId,
        @NotNull @Min(0) Long expectedVersion,
        @NotNull @Min(1) Long expectedMembershipVersion,
        @NotBlank @Size(max = 1000) String reason,
        @NotBlank @Size(max = 200) String verificationEvidenceReference,
        @NotNull UUID idempotencyKey,
        @NotNull @AssertTrue Boolean confirm) { }

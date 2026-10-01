package com.olarize.employment.infrastructure.in.web.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Full minimum work facts. Omitted optional values clear those fields. Unknown/private/payroll fields are rejected.")
public record CorrectEmployeeRequestDto(
        @NotBlank @Size(max = 64) String reference,
        @NotBlank @Size(max = 200) String name,
        @NotNull LocalDate startDate,
        @Size(max = 200) String jobTitle,
        @Size(max = 200) String department,
        @Size(max = 200) String site,
        @Email @Size(max = 254) String workEmail,
        @Size(max = 32) String workPhone,
        @NotNull LocalDate effectiveDate,
        @NotNull @Min(1) Long expectedVersion,
        @NotNull @Min(1) Long expectedEpisodeVersion,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull UUID idempotencyKey,
        @NotNull @AssertTrue Boolean confirm) { }

package com.olarize.employment.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
        @Schema(description = "Company-unique employee reference; leading zeroes are retained", example = "EMP-0007")
        @NotBlank @Size(max = 64) String reference,
        @Schema(description = "Employee's supplied legal or display name", example = "Salma El Idrissi")
        @NotBlank @Size(max = 200) String name,
        @Schema(description = "First employed civil day, YYYY-MM-DD", example = "2026-10-05")
        @NotNull LocalDate startDate,
        @Schema(description = "Optional work title", example = "People Operations Specialist")
        @Size(max = 200) String jobTitle,
        @Schema(description = "Optional department label", example = "People Operations")
        @Size(max = 200) String department,
        @Schema(description = "Optional site label", example = "Casablanca")
        @Size(max = 200) String site,
        @Schema(description = "Optional work email", example = "salma@demo.olarize.invalid")
        @Email @Size(max = 254) String workEmail,
        @Schema(description = "Optional work phone", example = "+212 5 22 00 00 00")
        @Size(max = 32) String workPhone,
        @Schema(description = "Unique UUID for this actor, company and exact request; reuse for a safe retry",
                example = "dcb466cc-22bb-4a6e-90c1-5e3ff5743480")
        @NotNull UUID idempotencyKey,
        @Schema(description = "HR has reviewed the exact facts and confirms creation", example = "true")
        @NotNull @AssertTrue Boolean confirm) { }

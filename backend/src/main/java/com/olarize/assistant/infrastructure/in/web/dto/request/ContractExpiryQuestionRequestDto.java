package com.olarize.assistant.infrastructure.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractExpiryQuestionRequestDto(
        @NotBlank @Size(max = 1000)
        @Schema(description = "Supported local synthetic question; other wording requires clarification", example = "Which contracts expire in October 2026?") String question,
        @Schema(description = "Zero-based page; default 0", example = "0") Integer page,
        @Schema(description = "Page size, 1–100; default 25", example = "25") Integer pageSize) { }

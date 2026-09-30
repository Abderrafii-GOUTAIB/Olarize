package com.olarize.assistant.infrastructure.in.web.dto.response;

import com.olarize.assistant.domain.model.ExpiryAnswerStatus;
import java.util.UUID;

public record ContractExpiryAnswerResponseDto(UUID conversationId, ExpiryAnswerStatus status, String message,
        ExpiryResultResponseDto result, String fallbackPath) { }

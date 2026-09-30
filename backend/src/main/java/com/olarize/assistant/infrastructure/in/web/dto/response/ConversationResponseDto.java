package com.olarize.assistant.infrastructure.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponseDto(UUID conversationId, UUID companyId, Instant expiresAt) { }

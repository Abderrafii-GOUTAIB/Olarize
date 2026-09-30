package com.olarize.assistant.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Conversation(UUID id, UUID companyId, String ownerUserId, Instant createdAt, Instant expiresAt) { }

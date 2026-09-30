package com.olarize.assistant.application.port.in;

import com.olarize.assistant.domain.model.ExpiryAnswerStatus;
import com.olarize.employment.application.port.in.ExpiringContractsResult;
import java.util.UUID;

public record ExpiryAnswer(UUID conversationId, ExpiryAnswerStatus status, String message, ExpiringContractsResult result) { }

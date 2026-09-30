package com.olarize.assistant.domain.model;

import java.util.Map;

public record ExpiryToolCall(String toolName, Map<String, String> arguments) {
    public ExpiryToolCall { arguments = Map.copyOf(arguments); }
}

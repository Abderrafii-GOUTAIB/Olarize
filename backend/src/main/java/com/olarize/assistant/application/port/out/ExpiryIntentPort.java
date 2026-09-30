package com.olarize.assistant.application.port.out;

import com.olarize.assistant.domain.model.ExpiryToolCall;

public interface ExpiryIntentPort { ExpiryToolCall plan(String question); }

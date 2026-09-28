package com.olarize.organization.internal.domain;

import java.util.UUID;

public record CompanyRegistration(String name, String reason, UUID idempotencyKey) {
    public CompanyRegistration {
        name = bounded(name, 200);
        reason = bounded(reason, 1000);
        if (idempotencyKey == null) throw new IllegalArgumentException("Invalid registration");
    }

    private static String bounded(String value, int maximum) {
        if (value == null) throw new IllegalArgumentException("Invalid registration");
        String normalized = value.strip();
        if (normalized.isBlank() || normalized.codePointCount(0, normalized.length()) > maximum
                || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid registration");
        }
        return normalized;
    }
}

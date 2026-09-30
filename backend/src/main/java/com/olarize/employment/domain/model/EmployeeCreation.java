package com.olarize.employment.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

/** Minimum factual record; dates and names do not imply a hiring decision. */
public record EmployeeCreation(String reference, String normalizedReference, String name,
        LocalDate startDate, String jobTitle, String department, String site,
        String workEmail, String workPhone) {
    public static EmployeeCreation of(String reference, String name, LocalDate startDate,
            String jobTitle, String department, String site, String workEmail, String workPhone) {
        String trimmedReference = required(reference, 64);
        String trimmedName = required(name, 200);
        if (startDate == null) throw new IllegalArgumentException("startDate");
        return new EmployeeCreation(trimmedReference, trimmedReference.toLowerCase(Locale.ROOT),
                trimmedName, startDate, optional(jobTitle, 200), optional(department, 200),
                optional(site, 200), optional(workEmail, 254), optional(workPhone, 32));
    }

    private static String required(String value, int limit) {
        if (value == null || value.isBlank() || value.trim().length() > limit) {
            throw new IllegalArgumentException("required field");
        }
        return value.trim();
    }

    private static String optional(String value, int limit) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() > limit || trimmed.isEmpty()) throw new IllegalArgumentException("optional field");
        return trimmed;
    }

    public EmploymentStatus status(Clock clock) {
        return startDate.isAfter(LocalDate.now(clock)) ? EmploymentStatus.UPCOMING : EmploymentStatus.CURRENT;
    }
}

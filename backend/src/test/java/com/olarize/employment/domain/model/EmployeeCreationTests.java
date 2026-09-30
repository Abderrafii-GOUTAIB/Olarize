package com.olarize.employment.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeCreationTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-29T22:30:00Z"),
            ZoneId.of("Africa/Casablanca"));

    @Test void keepsLeadingZeroesAndUnicodeAndDerivesCivilStatus() {
        var future = EmployeeCreation.of(" 0007 ", " سلمى الإدريسي ", LocalDate.of(2026, 10, 1),
                null, null, null, null, null);
        assertThat(future.reference()).isEqualTo("0007");
        assertThat(future.normalizedReference()).isEqualTo("0007");
        assertThat(future.name()).isEqualTo("سلمى الإدريسي");
        assertThat(future.status(clock)).isEqualTo(EmploymentStatus.UPCOMING);
        assertThat(EmployeeCreation.of("X", "Salma", LocalDate.of(2026, 9, 29),
                null, null, null, null, null).status(clock)).isEqualTo(EmploymentStatus.CURRENT);
    }

    @Test void rejectsMissingAndOversizedMinimumFacts() {
        assertThatThrownBy(() -> EmployeeCreation.of("  ", "Salma", LocalDate.now(clock),
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeCreation.of("EMP-1", " ", LocalDate.now(clock),
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeCreation.of("EMP-1", "Salma", null,
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeCreation.of("X".repeat(65), "Salma", LocalDate.now(clock),
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

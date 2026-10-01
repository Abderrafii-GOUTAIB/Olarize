package com.olarize.employment.domain.model;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmployeeCorrectionTests {
    private EmployeeCreation facts(String email, String phone) {
        return EmployeeCreation.of(" 0007 ", " سلمى الإدريسي ", LocalDate.parse("2026-01-01"),
                "Analyst", "People", null, email, phone);
    }
    @Test void normalizesUnicodeFactsAndKeepsLeadingZeroes() {
        var correction = new EmployeeCorrection(facts("salma@demo.invalid", "+212 5 22 00 00 00"),
                LocalDate.parse("2026-02-01"), " Reviewed source ");
        assertThat(correction.facts().reference()).isEqualTo("0007");
        assertThat(correction.facts().name()).isEqualTo("سلمى الإدريسي");
        assertThat(correction.reason()).isEqualTo("Reviewed source");
    }
    @Test void rejectsInvalidContactsReasonAndDateWithoutTruncation() {
        assertThatThrownBy(() -> new EmployeeCorrection(facts("not-email", null), LocalDate.parse("2026-01-01"), "Reason"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeCorrection(facts(null, "abc123"), LocalDate.parse("2026-01-01"), "Reason"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeCorrection(facts(null, null), LocalDate.parse("2025-12-31"), "Reason"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeCorrection(facts(null, null), LocalDate.parse("2026-01-01"), " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeCorrection(facts(null, null), LocalDate.parse("2026-01-01"), "x".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeCreation.of("r", " ", LocalDate.now(), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void identifiesDependentStartConflictsAndAllowsInclusiveBoundaries() {
        LocalDate start = LocalDate.parse("2026-03-01");
        assertThat(EmployeeCorrection.startConflicts(start, start.minusDays(1), List.of(start.minusDays(1)), true))
                .containsExactly("LAST_EMPLOYED_DAY", "JOB_INTERVAL", "OTHER_EPISODE");
        assertThat(EmployeeCorrection.startConflicts(start, start, List.of(start), false)).isEmpty();
    }
}

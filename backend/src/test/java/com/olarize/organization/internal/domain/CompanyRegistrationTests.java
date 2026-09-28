package com.olarize.organization.internal.domain;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CompanyRegistrationTests {
    @Test void normalizesUnicodeWithoutInventingLegalIdentifiers() {
        var value = new CompanyRegistration("  شركة Démo  ", "  Synthetic setup  ", UUID.randomUUID());
        assertThat(value.name()).isEqualTo("شركة Démo");
        assertThat(value.reason()).isEqualTo("Synthetic setup");
    }

    @Test void rejectsMissingAndOversizedValues() {
        for (String name : new String[]{null, "", " \t ", "a".repeat(201), "name\0"}) {
            assertThatThrownBy(() -> new CompanyRegistration(name, "reason", UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid registration");
        }
        for (String reason : new String[]{null, "", " ", "r".repeat(1001)}) {
            assertThatThrownBy(() -> new CompanyRegistration("company", reason, UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new CompanyRegistration("company", "reason", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void countsUnicodeCodePointsAtBoundaries() {
        assertThat(new CompanyRegistration("😀".repeat(200), "r".repeat(1000), UUID.randomUUID()).name())
                .isEqualTo("😀".repeat(200));
    }
}

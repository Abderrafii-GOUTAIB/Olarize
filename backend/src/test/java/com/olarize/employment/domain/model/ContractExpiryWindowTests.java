package com.olarize.employment.domain.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractExpiryWindowTests {
    @Test void acceptsOneDayAndMaximumInclusiveWindowIncludingLeapDay() {
        assertThat(new ContractExpiryWindow(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 1)))
                .isNotNull();
        assertThat(new ContractExpiryWindow(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28)))
                .isNotNull();
        assertThat(new ContractExpiryWindow(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
                .isNotNull();
        assertThat(new ContractExpiryWindow(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1)))
                .isNotNull();
    }

    @Test void rejectsMissingReversedAndOverlongRanges() {
        assertThatThrownBy(() -> new ContractExpiryWindow(null, LocalDate.of(2027, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContractExpiryWindow(LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContractExpiryWindow(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

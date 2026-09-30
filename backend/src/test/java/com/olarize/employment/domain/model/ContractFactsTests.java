package com.olarize.employment.domain.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractFactsTests {
    @Test void acceptsRecordedFactsWithoutAssigningLegalReview() {
        var facts = ContractFacts.of(ContractType.CDD, LocalDate.of(2026, 1, 1),
                null, "  HR archive item 12  ");

        assertThat(facts.typeCode()).isEqualTo(ContractType.CDD);
        assertThat(facts.endDate()).isNull();
        assertThat(facts.sourceReference()).isEqualTo("HR archive item 12");
    }

    @Test void rejectsMissingFactsInvalidIntervalsAndOversizedSourceReferences() {
        assertThatThrownBy(() -> ContractFacts.of(null, LocalDate.of(2026, 1, 1), null, "source"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContractFacts.of(ContractType.UNKNOWN, null, null, "source"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContractFacts.of(ContractType.CDI, LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 31), "source"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContractFacts.of(ContractType.CDI, LocalDate.of(2026, 1, 1),
                null, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContractFacts.of(ContractType.CDI, LocalDate.of(2026, 1, 1),
                null, "X".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

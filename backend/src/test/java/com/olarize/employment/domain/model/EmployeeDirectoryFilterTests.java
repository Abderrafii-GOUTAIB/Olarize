package com.olarize.employment.domain.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeDirectoryFilterTests {
    @Test void employmentDatesAreInclusiveAndDoNotUseContractDates() {
        var start = LocalDate.of(2026, 1, 1);
        var end = LocalDate.of(2026, 9, 30);
        assertThat(EmploymentStatus.asOf(start, end, start.minusDays(1))).isEqualTo(EmploymentStatus.UPCOMING);
        assertThat(EmploymentStatus.asOf(start, end, start)).isEqualTo(EmploymentStatus.CURRENT);
        assertThat(EmploymentStatus.asOf(start, end, end)).isEqualTo(EmploymentStatus.CURRENT);
        assertThat(EmploymentStatus.asOf(start, end, end.plusDays(1))).isEqualTo(EmploymentStatus.ENDED);
        assertThat(EmploymentStatus.asOf(start, null, end.plusYears(10))).isEqualTo(EmploymentStatus.CURRENT);
    }

    @Test void searchesTreatWildcardCharactersAsLiteralAndPreserveUnicode() {
        var filter = new EmployeeDirectoryFilter("  Ahmed_%!  ", EmployeeDirectoryStatus.ALL, 0, 25);
        assertThat(filter.searchPattern()).isEqualTo("%ahmed!_!%!!%");
        assertThat(new EmployeeDirectoryFilter("أحمد", EmployeeDirectoryStatus.CURRENT, 0, 25).searchPattern())
                .isEqualTo("%أحمد%");
        assertThat(new EmployeeDirectoryFilter("  ", EmployeeDirectoryStatus.CURRENT, 0, 25).search()).isNull();
    }

    @Test void rejectsUnboundedPaginationAndSearch() {
        assertThatThrownBy(() -> new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, -1, 25))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, Integer.MAX_VALUE, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, Integer.MAX_VALUE, 1).page())
                .isEqualTo(Integer.MAX_VALUE);
        assertThat(new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, Integer.MAX_VALUE / 100, 100).pageSize())
                .isEqualTo(100);
        assertThatThrownBy(() -> new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.ALL, Integer.MAX_VALUE / 100 + 1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeDirectoryFilter("a".repeat(201), EmployeeDirectoryStatus.ALL, 0, 25))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

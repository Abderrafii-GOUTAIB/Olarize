package com.olarize.assistant.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpiryWindowTests {
    private Clock clock(String instant) { return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC); }
    @Test void yearLeapMonthAndCasablancaMidnightUseZoneRules() {
        var call = new ExpiryToolCall("listExpiringContracts", Map.of("period", "NEXT_MONTH"));
        assertThat(ExpiryWindow.resolve(call, clock("2026-12-23T12:00:00Z")).fromDate()).hasToString("2027-01-01");
        assertThat(ExpiryWindow.resolve(call, clock("2028-01-23T12:00:00Z")).toDate()).hasToString("2028-02-29");
        assertThat(ExpiryWindow.resolve(call, clock("2026-09-30T23:30:00Z")).fromDate()).hasToString("2026-11-01");
    }
    @Test void toolNameContextOverridesUnknownAndInvalidArgumentsAreRejected() {
        var clock = clock("2026-09-23T12:00:00Z");
        for (var call : java.util.List.of(new ExpiryToolCall("executeSql", Map.of()),
                new ExpiryToolCall("listExpiringContracts", Map.of("period", "NEXT_MONTH", "companyId", "other-company")),
                new ExpiryToolCall("listExpiringContracts", Map.of("period", "NEXT_MONTH", "subject", "other-user")),
                new ExpiryToolCall("listExpiringContracts", Map.of("fromDate", "2026-10-31", "toDate", "2026-10-01")),
                new ExpiryToolCall("listExpiringContracts", Map.of("fromDate", "invalid", "toDate", "2026-10-01")))) {
            assertThatThrownBy(() -> ExpiryWindow.resolve(call, clock)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

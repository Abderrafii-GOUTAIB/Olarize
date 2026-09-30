package com.olarize.employment.adapter.out.persistence;

import com.olarize.employment.domain.model.ContractType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContractFactsPersistenceMapperTests {
    @Test void roundsRecordedAtToPostgresMicrosecondPrecision() {
        Instant recordedAt = Instant.parse("2026-09-30T06:39:04.991331900Z");
        var version = new EmploymentContractVersionJpaEntity(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ContractType.CDD,
                LocalDate.of(2026, 1, 1), null, "archive", "actor", "CREATE", null,
                null, "correlation", recordedAt);

        assertThat(ContractFactsPersistenceMapper.response(version).recordedAt())
                .isEqualTo(Instant.parse("2026-09-30T06:39:04.991332Z"));
    }

    @Test void roundsRecordedAtAcrossSecondBoundary() {
        Instant recordedAt = Instant.parse("2026-09-30T06:39:04.999999900Z");
        var version = new EmploymentContractVersionJpaEntity(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ContractType.CDD,
                LocalDate.of(2026, 1, 1), null, "archive", "actor", "CREATE", null,
                null, "correlation", recordedAt);

        assertThat(ContractFactsPersistenceMapper.response(version).recordedAt())
                .isEqualTo(Instant.parse("2026-09-30T06:39:05Z"));
    }
}

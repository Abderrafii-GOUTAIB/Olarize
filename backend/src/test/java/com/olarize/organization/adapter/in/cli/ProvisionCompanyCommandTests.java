package com.olarize.organization.adapter.in.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProvisionCompanyCommandTests {
    private static final Map<String, String> CONFIGURATION = Map.of(
            "ORG_PROVISION_DB_URL", "jdbc:no-such-synthetic-driver:organization-test",
            "ORG_PROVISION_DB_USERNAME", "synthetic-operator", "ORG_PROVISION_DB_PASSWORD", "synthetic-only");

    @Test void validInputReachesPersistenceRatherThanMissingConfiguration() {
        String input = "{\"name\":\"Synthetic\",\"reason\":\"Synthetic setup\",\"idempotencyKey\":\"11111111-1111-4111-8111-111111111111\",\"confirm\":true}";
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        int exit = ProvisionCompanyCommand.run(new String[]{"provision-company"},
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), new PrintStream(output), new PrintStream(errors), CONFIGURATION);
        assertThat(exit).isEqualTo(5);
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("ORG_FAILED").doesNotContain("synthetic-only", "jdbc:", "Synthetic setup");
    }

    @Test void rejectsUnconfirmedUnknownDuplicateMalformedAndOversizedInputWithoutPayloadLeakage() {
        String valid = "{\"name\":\"Private company\",\"reason\":\"Private reason\",\"idempotencyKey\":\"11111111-1111-4111-8111-111111111111\",\"confirm\":true}";
        for (String input : new String[]{"", "[]", valid.replace("true", "false"), valid.replace("true", "\"true\""),
                valid.replace("\"confirm\"", "\"actor\""), valid.replace("true}", "true,\"actor\":\"admin\"}"),
                valid.replace("true}", "true,\"name\":\"Other\"}"), valid + " {}", "x".repeat(16385)}) {
            var output = new ByteArrayOutputStream();
            var errors = new ByteArrayOutputStream();
            int exit = ProvisionCompanyCommand.run(new String[]{"provision-company"},
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), new PrintStream(output), new PrintStream(errors), CONFIGURATION);
            assertThat(exit).as(input.substring(0, Math.min(input.length(), 80))).isEqualTo(2);
            assertThat(output.toString()).isEmpty();
            assertThat(errors.toString()).contains("ORG_INVALID_INPUT", "requestId").doesNotContain("Private", "admin", "Exception");
        }
    }
}

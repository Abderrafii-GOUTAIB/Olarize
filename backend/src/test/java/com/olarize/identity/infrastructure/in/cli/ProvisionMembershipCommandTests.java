package com.olarize.identity.infrastructure.in.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class ProvisionMembershipCommandTests {
    private static final String VALID = "{\"companyId\":\"11111111-1111-4111-8111-111111111111\","
            + "\"subject\":\"synthetic-subject\",\"action\":\"GRANT\",\"expectedVersion\":0,"
            + "\"reason\":\"Synthetic setup\",\"evidence\":\"Synthetic verification reference\","
            + "\"idempotencyKey\":\"22222222-2222-4222-8222-222222222222\",\"confirm\":true}";

    @Test void everyMissingFieldIsRejectedAsInvalidInputBeforeDatabaseAccess() throws Exception {
        JsonMapper mapper = new JsonMapper();
        ObjectNode complete = (ObjectNode) mapper.readTree(VALID);
        for (String field : Set.of("companyId", "subject", "action", "expectedVersion", "reason", "evidence", "idempotencyKey", "confirm")) {
            ObjectNode missing = complete.deepCopy();
            missing.remove(field);
            var stdout = new ByteArrayOutputStream();
            var stderr = new ByteArrayOutputStream();
            int exit = ProvisionMembershipCommand.run(new String[]{"provision-membership"},
                    new ByteArrayInputStream(mapper.writeValueAsBytes(missing)), new PrintStream(stdout),
                    new PrintStream(stderr), Map.of());
            assertThat(exit).as("missing %s", field).isEqualTo(2);
            assertThat(stdout.toString(StandardCharsets.UTF_8)).isEmpty();
            assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("IDENTITY_INVALID_INPUT", "requestId")
                    .doesNotContain("synthetic-subject", "Synthetic setup", "Exception", "sql");
        }
    }
}

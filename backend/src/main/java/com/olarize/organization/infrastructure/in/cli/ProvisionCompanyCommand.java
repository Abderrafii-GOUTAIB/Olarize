package com.olarize.organization.infrastructure.in.cli;

import com.olarize.organization.application.service.ProvisionCompanyService;
import com.olarize.organization.infrastructure.out.persistence.repository.CompanyRegistrationJdbcRepository;
import com.olarize.organization.domain.exception.RegistrationConflict;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Explicit one-shot process: no Spring context, web server, migrations or fallback web credentials. */
public final class ProvisionCompanyCommand {
    private static final int MAX_INPUT_BYTES = 16_384;
    private static final Set<String> FIELDS = Set.of("name", "reason", "idempotencyKey", "confirm");
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private ProvisionCompanyCommand() { }

    public static int run(String[] args, InputStream input, PrintStream output, PrintStream errors, Map<String, String> environment) {
        UUID correlation = UUID.randomUUID();
        try {
            if (args.length != 1 || !"provision-company".equals(args[0])) throw new IllegalArgumentException();
            byte[] bytes = input.readNBytes(MAX_INPUT_BYTES + 1);
            if (bytes.length > MAX_INPUT_BYTES) throw new IllegalArgumentException();
            JsonNode request = JSON.readTree(bytes);
            if (request == null || !request.isObject() || !request.propertyNames().equals(FIELDS)
                    || !request.get("confirm").isBoolean() || !request.get("confirm").booleanValue()) {
                throw new IllegalArgumentException();
            }
            for (String field : Set.of("name", "reason", "idempotencyKey")) {
                if (!request.get(field).isString()) throw new IllegalArgumentException();
            }
            String keyText = request.get("idempotencyKey").stringValue();
            UUID key = UUID.fromString(keyText);
            if (!key.toString().equalsIgnoreCase(keyText)) throw new IllegalArgumentException();
            // Validate before connecting, so invalid requests cannot perform any database work.
            var registration = new com.olarize.organization.domain.model.CompanyRegistration(
                    request.get("name").stringValue(), request.get("reason").stringValue(), key);
            var datasource = new DriverManagerDataSource(required(environment, "ORG_PROVISION_DB_URL"),
                    required(environment, "ORG_PROVISION_DB_USERNAME"), required(environment, "ORG_PROVISION_DB_PASSWORD"));
            var company = new ProvisionCompanyService(new CompanyRegistrationJdbcRepository(datasource))
                    .execute(registration.name(), registration.reason(), key, correlation);
            output.println(JSON.writeValueAsString(Map.of("companyId", company.id().toString(),
                    "version", company.version(), "requestId", correlation.toString())));
            return 0;
        } catch (RegistrationConflict failure) {
            return error(errors, "ORG_CONFLICT", correlation, 3);
        } catch (SecurityException failure) {
            return error(errors, "ORG_DENIED", correlation, 4);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException failure) {
            return error(errors, "ORG_INVALID_INPUT", correlation, 2);
        } catch (Exception failure) {
            return error(errors, "ORG_FAILED", correlation, 5);
        }
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
        return value;
    }

    private static int error(PrintStream output, String code, UUID correlation, int exit) {
        output.println("{\"code\":\"" + code + "\",\"requestId\":\"" + correlation + "\"}");
        return exit;
    }
}

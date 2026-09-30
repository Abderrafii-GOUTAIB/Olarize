package com.olarize.identity.infrastructure.in.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Restricted process adapter. Database function owns authorization and atomic writes. */
public final class ProvisionMembershipCommand {
    private static final JsonMapper JSON = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private static final Set<String> FIELDS = Set.of("companyId", "subject", "action", "expectedVersion", "reason", "evidence", "idempotencyKey", "confirm");
    private ProvisionMembershipCommand() { }

    public static int run(String[] args, InputStream input, PrintStream output, PrintStream errors, Map<String, String> env) {
        UUID correlation = UUID.randomUUID();
        try {
            if (args.length != 1 || !"provision-membership".equals(args[0])) throw new IllegalArgumentException();
            byte[] bytes = input.readNBytes(16385);
            if (bytes.length > 16384) throw new IllegalArgumentException();
            JsonNode request = JSON.readTree(bytes);
            if (request == null || !request.isObject() || !request.propertyNames().equals(FIELDS)
                    || !request.get("confirm").isBoolean() || !request.get("confirm").booleanValue()
                    || !request.get("expectedVersion").isIntegralNumber() || !request.get("expectedVersion").canConvertToLong()
                    || request.get("expectedVersion").longValue() < 0) throw new IllegalArgumentException();
            for (String field : Set.of("companyId", "subject", "action", "reason", "evidence", "idempotencyKey")) {
                if (!request.get(field).isString()) throw new IllegalArgumentException();
            }
            UUID company = uuid(request.get("companyId").stringValue());
            UUID key = uuid(request.get("idempotencyKey").stringValue());
            String subject = bounded(request.get("subject").stringValue(), 255);
            String reason = bounded(request.get("reason").stringValue(), 1000);
            String evidence = bounded(request.get("evidence").stringValue(), 1000);
            String action = request.get("action").stringValue();
            if (!Set.of("GRANT", "REVOKE").contains(action)) throw new IllegalArgumentException();
            String url = required(env, "IDENTITY_PROVISION_DB_URL");
            String user = required(env, "IDENTITY_PROVISION_DB_USERNAME");
            String password = required(env, "IDENTITY_PROVISION_DB_PASSWORD");
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.prepareStatement("select * from public.provision_identity_membership(?,?,?,?,?,?,?,?)")) {
                statement.setObject(1, company); statement.setString(2, subject); statement.setString(3, action);
                statement.setLong(4, request.get("expectedVersion").longValue()); statement.setString(5, reason);
                statement.setString(6, evidence); statement.setObject(7, key); statement.setObject(8, correlation);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException();
                    output.println(JSON.writeValueAsString(Map.of("membershipId", result.getString("membership_id"),
                        "companyId", result.getString("company_id"), "version", result.getLong("version"),
                        "status", result.getString("status"), "requestId", correlation.toString())));
                }
            }
            return 0;
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException invalid) {
            return error(errors, "IDENTITY_INVALID_INPUT", correlation, 2);
        } catch (SQLException failure) {
            return switch (failure.getSQLState() == null ? "" : failure.getSQLState()) {
                case "42501", "28000", "28P01" -> error(errors, "IDENTITY_DENIED", correlation, 4);
                case "P0002", "23505" -> error(errors, "IDENTITY_CONFLICT", correlation, 3);
                case "22023" -> error(errors, "IDENTITY_INVALID_INPUT", correlation, 2);
                default -> error(errors, "IDENTITY_FAILED", correlation, 5);
            };
        } catch (Exception failure) { return error(errors, "IDENTITY_FAILED", correlation, 5); }
    }
    private static String bounded(String value, int max) {
        if (value.isBlank() || value.codePointCount(0, value.length()) > max) throw new IllegalArgumentException();
        return value;
    }
    private static UUID uuid(String value) {
        UUID result = UUID.fromString(value);
        if (!result.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        return result;
    }
    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
        return value;
    }
    private static int error(PrintStream errors, String code, UUID correlation, int exit) {
        errors.println("{\"code\":\"" + code + "\",\"requestId\":\"" + correlation + "\"}");
        return exit;
    }
}

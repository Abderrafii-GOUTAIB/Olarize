package com.olarize.platform.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Signs the authenticated request scope before an RLS-protected transaction reads data. */
@Component
public final class DatabaseScopeSigner {
    private final byte[] key;

    public DatabaseScopeSigner(@Value("${olarize.identity.database-scope-key:}") String hexKey) {
        if (hexKey == null || !hexKey.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("DB_SCOPE_KEY must be 64 hexadecimal characters");
        }
        this.key = HexFormat.of().parseHex(hexKey);
    }

    public void apply(JdbcTemplate database, String verifiedSubject, UUID companyId) {
        if (verifiedSubject == null || verifiedSubject.isBlank() || verifiedSubject.length() > 255) {
            throw new IllegalArgumentException("Verified subject is required");
        }
        String company = companyId == null ? "" : companyId.toString();
        String expires = Long.toString(Instant.now().plusSeconds(60).getEpochSecond());
        String message = verifiedSubject + "\n" + company + "\n" + expires;
        String proof;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            proof = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Database scope signing is unavailable", exception);
        }
        database.queryForObject("select set_config('olarize.subject', ?, true)", String.class, verifiedSubject);
        database.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, company);
        database.queryForObject("select set_config('olarize.scope_expires', ?, true)", String.class, expires);
        database.queryForObject("select set_config('olarize.scope_proof', ?, true)", String.class, proof);
    }
}

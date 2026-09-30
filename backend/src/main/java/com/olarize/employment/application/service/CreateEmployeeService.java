package com.olarize.employment.application.service;

import com.olarize.employment.application.port.in.CreateEmployeeUseCase;
import com.olarize.employment.application.port.out.EmployeeCreationPort;
import com.olarize.employment.domain.model.EmployeeCreation;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreateEmployeeService implements CreateEmployeeUseCase {
    private final CompanyAccessUseCase access;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final EmployeeCreationPort records;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public CreateEmployeeService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, EmployeeCreationPort records, PlatformTransactionManager transactions) {
        this.access = access;
        this.scope = scope;
        this.database = database;
        this.records = records;
        this.clock = Clock.system(java.time.ZoneId.of("Africa/Casablanca"));
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public CreatedEmployee create(UUID companyId, EmployeeCreation facts, UUID key, String correlationId) {
        if (companyId == null || facts == null || key == null) throw new IllegalArgumentException("Missing creation input");
        access.require(companyId, "employee.write");
        String actor = access.currentUserId();
        return transaction.execute(status -> createInTransaction(companyId, facts, key, correlationId, actor));
    }

    private CreatedEmployee createInTransaction(UUID companyId, EmployeeCreation facts,
            UUID key, String correlationId, String actor) {
        scope.apply(database, actor, companyId);
        records.serializeRetry(companyId, actor, key);
        String hash = hash(facts);
        var receipt = records.findReceipt(companyId, actor, key)
                .orElseGet(() -> records.insert(companyId, actor, key, hash, facts, correlationId, Instant.now(clock)));
        if (!receipt.payloadHash().equals(hash)) throw new EmployeeCreationConflict();
        var result = receipt.facts();
        return new CreatedEmployee(receipt.employeeId(), receipt.episodeId(), companyId,
                result.reference(), result.name(), result.startDate(), result.status(clock), 1);
    }

    private static String hash(EmployeeCreation facts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{facts.reference(), facts.normalizedReference(), facts.name(),
                    facts.startDate().toString(), facts.jobTitle(), facts.department(), facts.site(),
                    facts.workEmail(), facts.workPhone()}) {
                update(digest, value);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}

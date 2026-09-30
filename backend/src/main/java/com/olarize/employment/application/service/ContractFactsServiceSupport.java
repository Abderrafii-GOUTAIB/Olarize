package com.olarize.employment.application.service;

import com.olarize.employment.application.port.in.ContractVersionResponse;
import com.olarize.employment.application.port.out.ContractFactsPort;
import com.olarize.employment.domain.model.ContractFacts;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.jdbc.core.JdbcTemplate;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;

abstract class ContractFactsServiceSupport {
    private final CompanyAccessUseCase access;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final ContractFactsPort contracts;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    ContractFactsServiceSupport(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, ContractFactsPort contracts,
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scope = scope;
        this.database = database;
        this.contracts = contracts;
        this.transactionManager = transactionManager;
    }

    protected ContractVersionResponse mutate(UUID companyId, String operation, UUID key,
            List<String> additionalFacts, ContractFacts facts,
            BiFunction<String, String, ContractVersionResponse> write) {
        if (companyId == null || key == null || facts == null) {
            throw new IllegalArgumentException("Required contract input is missing");
        }
        access.require(companyId, "contract.write");
        String actor = access.currentUserId();
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
                    scope.apply(database, actor, companyId);
                    contracts.serializeRetry(companyId, actor, operation, key);
                    String hash = hash(operation, additionalFacts, facts);
                    var prior = contracts.findRetry(companyId, actor, operation, key);
                    if (prior.isPresent()) {
                        if (!prior.get().payloadHash().equals(hash)) throw new ContractFactsConflict();
                        return prior.get().result();
                    }
                    return write.apply(hash, actor);
                });
    }

    private static String hash(String operation, List<String> additional, ContractFacts facts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, operation);
            for (String value : additional) update(digest, value);
            update(digest, facts.typeCode().name());
            update(digest, facts.startDate().toString());
            update(digest, facts.endDate() == null ? null : facts.endDate().toString());
            update(digest, facts.sourceReference());
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

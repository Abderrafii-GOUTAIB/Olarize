package com.olarize.employment.application.service;

import com.olarize.employment.application.exception.EmployeeCorrectionConflict;
import com.olarize.employment.application.port.in.CorrectEmployeeUseCase;
import com.olarize.employment.application.port.out.EmployeeCorrectionPort;
import com.olarize.employment.domain.model.EmployeeCorrection;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CorrectEmployeeService implements CorrectEmployeeUseCase {
    private final CompanyAccessUseCase access;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final EmployeeCorrectionPort records;
    private final TransactionTemplate transaction;

    public CorrectEmployeeService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, EmployeeCorrectionPort records, PlatformTransactionManager transactions) {
        this.access = access;
        this.scope = scope;
        this.database = database;
        this.records = records;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public Result correct(UUID companyId, UUID employeeId, UUID episodeId, long expectedVersion,
            long expectedEpisodeVersion, EmployeeCorrection correction, UUID key, String correlationId) {
        if (companyId == null || employeeId == null || episodeId == null || correction == null
                || key == null || expectedVersion < 1 || expectedEpisodeVersion < 1) {
            throw new IllegalArgumentException("Missing correction input");
        }
        access.require(companyId, "employee.write");
        String actor = access.currentUserId();
        return transaction.execute(status -> {
            scope.apply(database, actor, companyId);
            records.serializeRetry(companyId, actor, key);
            String hash = hash(employeeId, episodeId, expectedVersion, expectedEpisodeVersion, correction);
            var previous = records.findRetry(companyId, actor, key);
            if (previous.isPresent()) {
                if (!previous.get().hash().equals(hash)) throw new EmployeeCorrectionConflict();
                return previous.get().result();
            }
            return records.correct(companyId, employeeId, episodeId, expectedVersion,
                    expectedEpisodeVersion, correction, actor, key, hash, correlationId, Instant.now());
        });
    }

    private static String hash(UUID employee, UUID episode, long version, long episodeVersion,
            EmployeeCorrection correction) {
        var f = correction.facts();
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{employee.toString(), episode.toString(), Long.toString(version),
                    Long.toString(episodeVersion), f.reference(), f.name(), f.startDate().toString(),
                    f.jobTitle(), f.department(), f.site(), f.workEmail(), f.workPhone(),
                    correction.effectiveDate().toString(), correction.reason()}) {
                byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(value == null ? -1 : bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 unavailable", unavailable); }
    }
}

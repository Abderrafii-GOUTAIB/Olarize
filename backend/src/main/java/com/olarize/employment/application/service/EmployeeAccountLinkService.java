package com.olarize.employment.application.service;

import com.olarize.employment.application.exception.EmployeeAccountLinkConflict;
import com.olarize.employment.application.exception.EmployeeAccountLinkUnavailable;
import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase;
import com.olarize.employment.application.port.out.EmployeeAccountLinkPort;
import com.olarize.employment.domain.model.EmployeeAccountLinkAction;
import com.olarize.employment.domain.model.EmployeeAccountLinkChange;
import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.identity.application.port.in.EmployeeLinkIdentityUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmployeeAccountLinkService implements EmployeeAccountLinkUseCase {
    private final CompanyAccessUseCase access;
    private final EmployeeLinkIdentityUseCase identity;
    private final EmployeeAccountLinkPort links;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final TransactionTemplate transaction;
    public EmployeeAccountLinkService(CompanyAccessUseCase access, EmployeeLinkIdentityUseCase identity,
            EmployeeAccountLinkPort links, DatabaseScopeSigner scope, JdbcTemplate database, PlatformTransactionManager manager) {
        this.access = access; this.identity = identity; this.links = links;
        this.scope = scope; this.database = database; this.transaction = new TransactionTemplate(manager);
    }
    @Override public Result read(UUID companyId, UUID membershipId) {
        access.require(companyId, "identity.link.admin");
        if (identity.inspect(companyId, membershipId) == null) throw new EmployeeAccountLinkUnavailable();
        return scoped(companyId, access.currentUserId(), () -> links.find(companyId, membershipId)
                .orElse(new Result(companyId, membershipId, null, EmployeeAccountLinkStatus.UNLINKED, 0, null)));
    }
    @Override public OwnLink own(UUID companyId) {
        var company = access.active(companyId);
        return scoped(company.companyId(), access.currentUserId(), () -> {
            var link = links.find(company.companyId(), company.membershipId())
                    .filter(result -> result.status() == EmployeeAccountLinkStatus.LINKED)
                    .orElseThrow(EmployeeAccountLinkUnavailable::new);
            return new OwnLink(company.companyId(), link.employeeId());
        });
    }
    @Override public Result change(UUID companyId, UUID membershipId, EmployeeAccountLinkChange change,
            UUID key, String correlationId) {
        if (companyId == null || membershipId == null || change == null || key == null) {
            throw new IllegalArgumentException("Missing link input");
        }
        access.require(companyId, "identity.link.admin");
        String actor = access.currentUserId();
        String hash = hash(membershipId, change);
        var previous = scoped(companyId, actor, () -> retry(companyId, actor, key, hash));
        if (previous != null) return previous;
        var target = identity.inspect(companyId, membershipId);
        if (target == null) throw new EmployeeAccountLinkUnavailable();
        if (actor.equals(target.subject())) throw new AccessDeniedException("Self linking denied");
        if (change.action() != EmployeeAccountLinkAction.REVOKE) {
            if (!target.active()) throw new EmployeeAccountLinkConflict();
            // External identity verification runs without a database transaction/lock.
            if (!identity.verifyAccount(target)) throw new EmployeeAccountLinkUnavailable();
        }
        return scoped(companyId, actor, () -> {
            var receipt = retry(companyId, actor, key, hash);
            if (receipt != null) return receipt;
            var current = identity.lock(companyId, membershipId);
            if (current == null) throw new EmployeeAccountLinkUnavailable();
            if (actor.equals(current.subject())) throw new AccessDeniedException("Self linking denied");
            if (current.version() != change.expectedMembershipVersion() || current.version() != target.version()
                    || !current.subject().equals(target.subject())
                    || (change.action() != EmployeeAccountLinkAction.REVOKE && !current.active())) {
                throw new EmployeeAccountLinkConflict();
            }
            if (change.employeeId() != null && !links.employeeExists(companyId, change.employeeId())) {
                throw new EmployeeAccountLinkUnavailable();
            }
            return links.mutate(companyId, membershipId, change, actor, key, hash, correlationId,
                    Instant.now().truncatedTo(ChronoUnit.MICROS));
        });
    }
    private Result retry(UUID companyId, String actor, UUID key, String hash) {
        links.serialize(companyId, actor, key);
        var prior = links.retry(companyId, actor, key);
        if (prior.isEmpty()) return null;
        if (!hash.equals(prior.get().hash())) throw new EmployeeAccountLinkConflict();
        return prior.get().result();
    }
    private <T> T scoped(UUID companyId, String actor, Supplier<T> operation) {
        return transaction.execute(status -> { scope.apply(database, actor, companyId); return operation.get(); });
    }
    private static String hash(UUID membership, EmployeeAccountLinkChange change) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{membership.toString(), change.action().name(),
                    change.employeeId() == null ? null : change.employeeId().toString(),
                    Long.toString(change.expectedVersion()), Long.toString(change.expectedMembershipVersion()),
                    change.reason(), change.verificationEvidenceReference()}) {
                byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(value == null ? -1 : bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 unavailable", unavailable); }
    }
}

package com.olarize.identity.adapter.out.persistence;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.application.port.out.MembershipAdministrationPort;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.InvalidMembershipPreview;
import com.olarize.identity.domain.exception.MembershipChangeRejected;
import com.olarize.identity.domain.model.Membership;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class MembershipAdministrationJdbcRepository implements MembershipAdministrationPort {
    private final JdbcTemplate database;
    private final DatabaseScopeSigner scope;

    MembershipAdministrationJdbcRepository(JdbcTemplate database, DatabaseScopeSigner scope) {
        this.database = database;
        this.scope = scope;
    }

    @Override @Transactional(readOnly = true)
    public List<Membership> list(String actorSubject, UUID companyId) {
        try {
            scope.apply(database, actorSubject, companyId);
            return database.query("select * from public.list_company_identity_memberships(?, ?)",
                    MembershipJdbcMapper.ADMINISTRATION_RESULT, actorSubject, companyId);
        } catch (DataAccessException failure) {
            throw translate(failure);
        }
    }

    @Override @Transactional(readOnly = true)
    public Membership findCommittedRetry(String actorSubject, MembershipChangePreviewClaims preview, UUID idempotencyKey) {
        try {
            scope.apply(database, actorSubject, preview.companyId());
            List<Membership> committed = database.query("""
                    select * from public.find_company_membership_change_retry(?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, MembershipJdbcMapper.ADMINISTRATION_RESULT, actorSubject, preview.companyId(), preview.targetSubject(),
                    preview.action().name(), preview.roleBundle() == null ? null : preview.roleBundle().name(),
                    preview.actorVersion(), preview.targetVersion(), preview.reason(), idempotencyKey);
            if (committed.size() > 1) throw new IllegalStateException("Idempotency key returned multiple results");
            return committed.isEmpty() ? null : committed.getFirst();
        } catch (DataAccessException failure) {
            throw translate(failure);
        }
    }

    @Override @Transactional
    public Membership change(String actorSubject, MembershipChangePreviewClaims preview,
            UUID idempotencyKey, String correlationId) {
        try {
            scope.apply(database, actorSubject, preview.companyId());
            return database.queryForObject("""
                    select * from public.change_company_membership(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, MembershipJdbcMapper.ADMINISTRATION_RESULT, actorSubject, preview.companyId(), preview.targetSubject(),
                    preview.action().name(), preview.roleBundle() == null ? null : preview.roleBundle().name(),
                    preview.actorVersion(), preview.targetVersion(), preview.reason(), idempotencyKey, correlationId);
        } catch (DataAccessException failure) {
            throw translate(failure);
        }
    }

    private static RuntimeException translate(DataAccessException failure) {
        Throwable cause = failure;
        while (cause != null && !(cause instanceof SQLException)) cause = cause.getCause();
        if (cause instanceof SQLException sql) {
            return switch (sql.getSQLState()) {
                case "42501" -> new CompanyAccessDenied();
                case "22023" -> new MembershipChangeRejected();
                case "P0002", "23505" -> new InvalidMembershipPreview();
                default -> failure;
            };
        }
        return failure;
    }
}

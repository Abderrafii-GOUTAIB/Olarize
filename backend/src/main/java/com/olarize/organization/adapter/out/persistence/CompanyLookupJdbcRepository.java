package com.olarize.organization.adapter.out.persistence;

import com.olarize.organization.application.port.in.CompanyIdentity;
import com.olarize.organization.application.port.in.CompanyLookupPort;
import com.olarize.organization.application.port.in.CompanyReadAuthorizationPort;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class CompanyLookupJdbcRepository implements CompanyLookupPort {
    private final JdbcTemplate database;
    private final ObjectProvider<CompanyReadAuthorizationPort> authorization;
    private final DatabaseScopeSigner scope;
    private final TransactionTemplate companyRead;

    public CompanyLookupJdbcRepository(JdbcTemplate database, ObjectProvider<CompanyReadAuthorizationPort> authorization,
                             DatabaseScopeSigner scope,
                             PlatformTransactionManager transactionManager) {
        this.database = database;
        this.authorization = authorization;
        this.scope = scope;
        this.companyRead = new TransactionTemplate(transactionManager);
        this.companyRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.companyRead.setReadOnly(true);
    }

    @Override
    public Optional<CompanyIdentity> find(UUID companyId) {
        CompanyReadAuthorizationPort verifier = authorization.getIfUnique();
        if (companyId == null || verifier == null) return Optional.empty();
        Optional<String> verifiedSubject = verifier.verifiedSubjectFor(companyId);
        if (verifiedSubject.isEmpty()) return Optional.empty();
        return companyRead.execute(status -> {
            scope.apply(database, verifiedSubject.get(), companyId);
            return database.query("select id, name, version, created_at, updated_at from public.organization_company where id = ?",
                    (row, index) -> new CompanyIdentity(row.getObject(1, UUID.class), row.getString(2), row.getLong(3),
                            row.getTimestamp(4).toInstant(), row.getTimestamp(5).toInstant()), companyId)
                    .stream().findFirst();
        });
    }
}

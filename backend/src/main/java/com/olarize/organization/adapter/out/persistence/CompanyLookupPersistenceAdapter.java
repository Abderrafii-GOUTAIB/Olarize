package com.olarize.organization.adapter.out.persistence;

import com.olarize.organization.application.port.in.CompanyIdentity;
import com.olarize.organization.application.port.in.CompanyLookupPort;
import com.olarize.organization.application.port.in.CompanyReadAuthorizationPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class CompanyLookupPersistenceAdapter implements CompanyLookupPort {
    private final JdbcTemplate database;
    private final ObjectProvider<CompanyReadAuthorizationPort> authorization;
    private final TransactionTemplate companyRead;

    public CompanyLookupPersistenceAdapter(JdbcTemplate database, ObjectProvider<CompanyReadAuthorizationPort> authorization,
                             PlatformTransactionManager transactionManager) {
        this.database = database;
        this.authorization = authorization;
        this.companyRead = new TransactionTemplate(transactionManager);
        this.companyRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.companyRead.setReadOnly(true);
    }

    @Override
    public Optional<CompanyIdentity> find(UUID companyId) {
        CompanyReadAuthorizationPort verifier = authorization.getIfUnique();
        if (companyId == null || verifier == null || !verifier.mayRead(companyId)) return Optional.empty();
        return companyRead.execute(status -> {
            database.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, companyId.toString());
            return database.query("select id, name, version, created_at, updated_at from public.organization_company where id = ?",
                    (row, index) -> new CompanyIdentity(row.getObject(1, UUID.class), row.getString(2), row.getLong(3),
                            row.getTimestamp(4).toInstant(), row.getTimestamp(5).toInstant()), companyId)
                    .stream().findFirst();
        });
    }
}

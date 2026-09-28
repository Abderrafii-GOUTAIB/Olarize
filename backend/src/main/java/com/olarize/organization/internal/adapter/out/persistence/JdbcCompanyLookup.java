package com.olarize.organization.internal.adapter.out.persistence;

import com.olarize.organization.api.CompanyIdentity;
import com.olarize.organization.api.CompanyLookup;
import com.olarize.organization.api.CompanyReadAuthorization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcCompanyLookup implements CompanyLookup {
    private final JdbcTemplate database;
    private final ObjectProvider<CompanyReadAuthorization> authorization;

    public JdbcCompanyLookup(JdbcTemplate database, ObjectProvider<CompanyReadAuthorization> authorization) {
        this.database = database;
        this.authorization = authorization;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<CompanyIdentity> find(UUID companyId) {
        CompanyReadAuthorization verifier = authorization.getIfUnique();
        if (companyId == null || verifier == null || !verifier.mayRead(companyId)) return Optional.empty();
        database.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, companyId.toString());
        return database.query("select id, name, version, created_at, updated_at from public.organization_company where id = ?",
                (row, index) -> new CompanyIdentity(row.getObject(1, UUID.class), row.getString(2), row.getLong(3),
                        row.getTimestamp(4).toInstant(), row.getTimestamp(5).toInstant()), companyId)
                .stream().findFirst();
    }
}

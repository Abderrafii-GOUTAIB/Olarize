package com.olarize.organization.infrastructure.out.persistence.repository;

import com.olarize.organization.application.port.in.CompanyIdentity;
import com.olarize.organization.application.port.out.CompanyRegistrationPort;
import com.olarize.organization.domain.model.CompanyRegistration;
import com.olarize.organization.domain.exception.RegistrationConflict;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

/** Dedicated command datasource only. Never registered as a web application bean. */
public final class CompanyRegistrationJdbcRepository implements CompanyRegistrationPort {
    private final DataSource datasource;

    public CompanyRegistrationJdbcRepository(DataSource datasource) { this.datasource = datasource; }

    @Override public CompanyIdentity register(CompanyRegistration input, UUID correlationId) {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String operator = operator(connection);
                try (var provision = connection.prepareStatement("select * from public.provision_company(?, ?, ?, ?, ?)")) {
                    provision.setObject(1, UUID.randomUUID());
                    provision.setString(2, input.name());
                    provision.setString(3, input.reason());
                    provision.setObject(4, input.idempotencyKey());
                    provision.setObject(5, correlationId);
                    try (var result = provision.executeQuery()) {
                        if (!result.next()) throw new SQLException("Company provision returned no result");
                        if (result.getBoolean("conflict")) throw new RegistrationConflict();
                        CompanyIdentity company = new CompanyIdentity(
                                result.getObject("company_id", UUID.class), result.getString("name"),
                                result.getLong("version"), result.getTimestamp("created_at").toInstant(),
                                result.getTimestamp("updated_at").toInstant());
                        connection.commit();
                        return company;
                    }
                }
            } catch (RuntimeException | SQLException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            // Do not propagate SQL text, credentials or company payload in diagnostics.
            throw new IllegalStateException("Provisioning persistence failed");
        }
    }

    private static String operator(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select session_user, r.rolsuper, r.rolbypassrls,
                    coalesce(pg_has_role(session_user, to_regrole('olarize_company_provisioner'), 'MEMBER'), false),
                    exists(select 1 from pg_class c where c.oid in
                        ('public.organization_company'::regclass, 'public.organization_company_creation'::regclass)
                        and pg_has_role(session_user, c.relowner, 'MEMBER'))
                from pg_roles r where r.rolname = session_user
                """); var rows = statement.executeQuery()) {
            if (!rows.next() || rows.getBoolean(2) || rows.getBoolean(3) || !rows.getBoolean(4) || rows.getBoolean(5)) {
                throw new SecurityException("Provisioning denied");
            }
            return rows.getString(1);
        }
    }

}

package com.olarize.identity.infrastructure.out.persistence.adapter;

import com.olarize.identity.application.port.in.EmployeeLinkIdentityUseCase.Target;
import com.olarize.identity.application.port.out.EmployeeLinkMembershipPort;
import com.olarize.platform.api.DatabaseScopeSigner;
import com.olarize.identity.application.port.out.AuthenticatedSubjectPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class EmployeeLinkMembershipJdbcAdapter implements EmployeeLinkMembershipPort {
    private final JdbcTemplate database;
    private final DatabaseScopeSigner scope;
    private final AuthenticatedSubjectPort actor;
    EmployeeLinkMembershipJdbcAdapter(JdbcTemplate database, DatabaseScopeSigner scope, AuthenticatedSubjectPort actor) {
        this.database = database; this.scope = scope; this.actor = actor;
    }
    @Override @Transactional
    public Optional<Target> find(UUID companyId, UUID membershipId, boolean lock) {
        scope.apply(database, actor.require(), companyId);
        // Narrow security-definer lookup/row lock preserves membership RLS and avoids
        // exposing all company memberships or HR fields to a link administrator.
        var rows = database.query("select * from public.olarize_employee_link_membership(?, ?, ?)",
                (row, index) -> new Target(row.getObject("membership_id", UUID.class), row.getString("subject"),
                        row.getLong("version"), "ACTIVE".equals(row.getString("status"))), companyId, membershipId, lock);
        return rows.stream().findFirst();
    }
}

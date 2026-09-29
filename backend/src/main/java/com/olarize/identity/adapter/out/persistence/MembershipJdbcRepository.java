package com.olarize.identity.adapter.out.persistence;

import com.olarize.identity.application.port.out.MembershipReaderPort;
import com.olarize.identity.domain.model.Membership;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MembershipJdbcRepository implements MembershipReaderPort {
    private final JdbcTemplate database;
    private final DatabaseScopeSigner scope;
    public MembershipJdbcRepository(JdbcTemplate database, DatabaseScopeSigner scope) {
        this.database = database;
        this.scope = scope;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Membership> activeFor(String subject) {
        if (subject == null || subject.isBlank()) return List.of();
        scope.apply(database, subject, null);
        return database.query("""
                select id, company_id, permissions, status, version, role_bundle, subject from public.identity_membership
                where subject = ? and status = 'ACTIVE' order by company_id
                """, MembershipJdbcMapper.ACTIVE_MEMBERSHIP, subject);
    }
}

package com.olarize.identity.adapter.out.persistence;

import com.olarize.identity.application.port.out.MembershipReaderPort;
import com.olarize.identity.domain.model.Membership;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MembershipPersistenceAdapter implements MembershipReaderPort {
    private final JdbcTemplate database;
    public MembershipPersistenceAdapter(JdbcTemplate database) { this.database = database; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Membership> activeFor(String subject) {
        if (subject == null || subject.isBlank()) return List.of();
        database.queryForObject("select set_config('olarize.subject', ?, true)", String.class, subject);
        return database.query("""
                select id, company_id, permissions, status, version, role_bundle, subject from public.identity_membership
                where subject = ? and status = 'ACTIVE' order by company_id
                """, (row, index) -> new Membership(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
                    Set.of((String[]) row.getArray(3).getArray()), row.getString(4), row.getLong(5),
                    row.getString(6) == null ? null : com.olarize.identity.domain.model.MembershipRoleBundle.valueOf(row.getString(6)),
                    row.getString(7)), subject);
    }
}

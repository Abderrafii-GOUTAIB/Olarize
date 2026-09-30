package com.olarize.identity.infrastructure.out.persistence.mapper;

import com.olarize.identity.domain.model.Membership;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

/** Maps the membership table and administration function result shapes. */
public final class MembershipJdbcMapper {
    public static final RowMapper<Membership> ACTIVE_MEMBERSHIP = (row, ignored) -> fromRow(row, "id");
    public static final RowMapper<Membership> ADMINISTRATION_RESULT = (row, ignored) -> fromRow(row, "membership_id");

    private MembershipJdbcMapper() { }

    private static Membership fromRow(ResultSet row, String idColumn) throws SQLException {
        String[] permissions = (String[]) row.getArray("permissions").getArray();
        String bundle = row.getString("role_bundle");
        return new Membership(row.getObject(idColumn, UUID.class), row.getObject("company_id", UUID.class),
                Set.of(permissions), row.getString("status"), row.getLong("version"),
                bundle == null ? null : MembershipRoleBundle.valueOf(bundle), row.getString("subject"));
    }
}

package com.olarize.identity.domain.model;

import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.CompanySelectionRequired;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record Membership(UUID id, UUID companyId, Set<String> permissions, String status, long version,
                         MembershipRoleBundle roleBundle, String subject) {
    public Membership(UUID id, UUID companyId, Set<String> permissions, String status, long version) {
        this(id, companyId, permissions, status, version, null, null);
    }
    public Membership { permissions = Set.copyOf(permissions); }
    public boolean active() { return "ACTIVE".equals(status); }

    public static Membership select(List<Membership> memberships, UUID requested) {
        var active = memberships.stream().filter(Membership::active).toList();
        if (requested != null) return active.stream().filter(m -> requested.equals(m.companyId()))
                .findFirst().orElseThrow(CompanyAccessDenied::new);
        if (active.isEmpty()) throw new CompanyAccessDenied();
        if (active.size() != 1) throw new CompanySelectionRequired();
        return active.getFirst();
    }
}

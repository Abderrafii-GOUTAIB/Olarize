package com.olarize.identity.adapter.in.web;

import com.olarize.identity.domain.model.Membership;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.util.Set;
import java.util.UUID;

public record ManagedMembershipResponse(UUID membershipId, String subject, UUID companyId, String status,
        MembershipRoleBundle roleBundle, Set<String> permissions, long version) {
    public ManagedMembershipResponse { permissions = Set.copyOf(permissions); }
    static ManagedMembershipResponse from(Membership member) {
        return new ManagedMembershipResponse(member.id(), member.subject(), member.companyId(), member.status(),
                member.roleBundle(), member.permissions(), member.version());
    }
}

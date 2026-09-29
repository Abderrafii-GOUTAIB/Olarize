package com.olarize.identity.adapter.in.web;

import com.olarize.identity.domain.model.Membership;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;

public record ManagedMembershipResponse(UUID membershipId,
        @Schema(description = "Keycloak ID of this member. Copy it into userId when previewing a change.")
        String userId, UUID companyId, String status,
        @Schema(description = "Current ordinary role. Null for a protected identity administrator membership.")
        MembershipRoleBundle roleBundle, Set<String> permissions, long version) {
    public ManagedMembershipResponse { permissions = Set.copyOf(permissions); }
    static ManagedMembershipResponse from(Membership member) {
        return new ManagedMembershipResponse(member.id(), member.subject(), member.companyId(), member.status(),
                member.roleBundle(), member.permissions(), member.version());
    }
}

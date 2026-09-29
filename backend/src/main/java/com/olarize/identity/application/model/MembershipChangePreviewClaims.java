package com.olarize.identity.application.model;

import com.olarize.identity.domain.model.MembershipChangeAction;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MembershipChangePreviewClaims(UUID previewId, String actorSubject, UUID companyId, String targetSubject,
        MembershipChangeAction action, MembershipRoleBundle roleBundle, Set<String> permissions,
        long actorVersion, long targetVersion, String reason, Instant expiresAt) {
    public MembershipChangePreviewClaims { permissions = Set.copyOf(permissions); }
}

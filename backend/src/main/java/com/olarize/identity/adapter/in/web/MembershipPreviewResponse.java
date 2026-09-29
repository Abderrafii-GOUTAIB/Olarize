package com.olarize.identity.adapter.in.web;

import com.olarize.identity.application.port.in.MembershipAdministrationUseCase;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MembershipPreviewResponse(String previewToken, UUID previewId, UUID companyId, String targetSubject,
        String action, MembershipRoleBundle roleBundle, Set<String> permissions, long actorVersion,
        long targetVersion, Instant expiresAt) {
    public MembershipPreviewResponse { permissions = Set.copyOf(permissions); }
    static MembershipPreviewResponse from(MembershipAdministrationUseCase.Preview preview) {
        return new MembershipPreviewResponse(preview.token(), preview.previewId(), preview.companyId(), preview.targetSubject(),
                preview.action(), preview.roleBundle(), preview.permissions(), preview.actorVersion(), preview.targetVersion(), preview.expiresAt());
    }
}

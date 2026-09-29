package com.olarize.identity.application.port.in;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.domain.model.Membership;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MembershipAdministrationUseCase {
    List<Membership> list(UUID companyId);
    Preview preview(UUID companyId, String targetSubject, MembershipRoleBundle roleBundle, String reason);
    Membership confirm(UUID companyId, String previewToken, long expectedTargetVersion, UUID idempotencyKey, String correlationId);

    record Preview(String token, UUID previewId, UUID companyId, String targetSubject, String action,
                   MembershipRoleBundle roleBundle, java.util.Set<String> permissions,
                   long actorVersion, long targetVersion, Instant expiresAt) {
        public Preview { permissions = java.util.Set.copyOf(permissions); }
    }
}

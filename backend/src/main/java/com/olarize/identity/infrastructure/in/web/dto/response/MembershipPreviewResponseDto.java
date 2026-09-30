package com.olarize.identity.infrastructure.in.web.dto.response;

import com.olarize.identity.domain.model.MembershipRoleBundle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MembershipPreviewResponseDto(String previewToken, UUID previewId, UUID companyId,
        @Schema(description = "Keycloak ID of the member this preview would change.") String userId,
        String action, MembershipRoleBundle roleBundle, Set<String> permissions, long actorVersion,
        long targetVersion, Instant expiresAt) {
    public MembershipPreviewResponseDto { permissions = Set.copyOf(permissions); }
}

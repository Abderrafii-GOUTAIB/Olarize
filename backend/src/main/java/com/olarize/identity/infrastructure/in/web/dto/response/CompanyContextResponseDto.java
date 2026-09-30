package com.olarize.identity.infrastructure.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record CompanyContextResponseDto(UUID membershipId, UUID companyId, String companyName,
        Set<String> permissions, long version) {
    public CompanyContextResponseDto { permissions = Set.copyOf(permissions); }
}

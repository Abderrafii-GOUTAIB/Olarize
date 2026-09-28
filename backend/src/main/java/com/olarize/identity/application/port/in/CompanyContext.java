package com.olarize.identity.application.port.in;

import java.util.Set;
import java.util.UUID;

/** Snapshot for the current request. Resolve again for each later operation. */
public record CompanyContext(UUID membershipId, UUID companyId, String companyName, Set<String> permissions, long version) {
    public CompanyContext { permissions = Set.copyOf(permissions); }
    public boolean permits(String permission) { return permissions.contains(permission); }
}

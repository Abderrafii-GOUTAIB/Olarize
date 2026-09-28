package com.olarize.organization.api;

import java.util.Optional;
import java.util.UUID;

public interface CompanyLookup {
    Optional<CompanyIdentity> find(UUID companyId);
}

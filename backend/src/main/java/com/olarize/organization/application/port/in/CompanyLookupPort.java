package com.olarize.organization.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface CompanyLookupPort {
    Optional<CompanyIdentity> find(UUID companyId);
}

package com.olarize.organization.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Implemented by identity using current authenticated membership, never request input alone. */
public interface CompanyReadAuthorizationPort {
    Optional<String> verifiedSubjectFor(UUID companyId);
}

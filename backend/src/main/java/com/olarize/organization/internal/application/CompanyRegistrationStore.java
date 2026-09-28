package com.olarize.organization.internal.application;

import com.olarize.organization.api.CompanyIdentity;
import com.olarize.organization.internal.domain.CompanyRegistration;
import java.util.UUID;

/** Atomic creation and audit, authenticated operator identity, and serialized retry handling. */
public interface CompanyRegistrationStore {
    CompanyIdentity register(CompanyRegistration registration, UUID correlationId);
}

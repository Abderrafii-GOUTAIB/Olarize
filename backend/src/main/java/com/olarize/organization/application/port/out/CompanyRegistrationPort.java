package com.olarize.organization.application.port.out;

import com.olarize.organization.application.port.in.CompanyIdentity;
import com.olarize.organization.domain.model.CompanyRegistration;
import java.util.UUID;

/** Atomic creation and audit, authenticated operator identity, and serialized retry handling. */
public interface CompanyRegistrationPort {
    CompanyIdentity register(CompanyRegistration registration, UUID correlationId);
}

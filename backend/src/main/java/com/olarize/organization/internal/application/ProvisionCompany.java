package com.olarize.organization.internal.application;

import com.olarize.organization.api.CompanyIdentity;
import com.olarize.organization.internal.domain.CompanyRegistration;
import java.util.UUID;

public final class ProvisionCompany {
    private final CompanyRegistrationStore store;

    public ProvisionCompany(CompanyRegistrationStore store) { this.store = store; }

    public CompanyIdentity execute(String name, String reason, UUID key, UUID correlationId) {
        return store.register(new CompanyRegistration(name, reason, key), correlationId);
    }
}

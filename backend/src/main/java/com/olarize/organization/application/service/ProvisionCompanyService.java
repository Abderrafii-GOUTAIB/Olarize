package com.olarize.organization.application.service;

import com.olarize.organization.application.port.in.CompanyIdentity;
import com.olarize.organization.application.port.out.CompanyRegistrationPort;
import com.olarize.organization.domain.model.CompanyRegistration;
import java.util.UUID;

public final class ProvisionCompanyService {
    private final CompanyRegistrationPort store;

    public ProvisionCompanyService(CompanyRegistrationPort store) { this.store = store; }

    public CompanyIdentity execute(String name, String reason, UUID key, UUID correlationId) {
        return store.register(new CompanyRegistration(name, reason, key), correlationId);
    }
}

package com.olarize.organization.application.port.in;

import java.util.UUID;

/** Implemented by identity using current authenticated membership, never request input alone. */
public interface CompanyReadAuthorizationPort {
    boolean mayRead(UUID companyId);
}

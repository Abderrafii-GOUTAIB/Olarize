package com.olarize.organization.api;

import java.util.UUID;

/** Implemented by identity using current authenticated membership, never request input alone. */
public interface CompanyReadAuthorization {
    boolean mayRead(UUID companyId);
}

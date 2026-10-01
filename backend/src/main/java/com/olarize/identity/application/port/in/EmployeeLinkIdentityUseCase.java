package com.olarize.identity.application.port.in;

import java.util.UUID;

/** Narrow verified membership metadata contract for employee-account linking. */
public interface EmployeeLinkIdentityUseCase {
    Target inspect(UUID companyId, UUID membershipId);
    boolean verifyAccount(Target target);
    Target lock(UUID companyId, UUID membershipId);
    class AccountVerificationUnavailable extends RuntimeException { }
    record Target(UUID membershipId, String subject, long version, boolean active) { }
}

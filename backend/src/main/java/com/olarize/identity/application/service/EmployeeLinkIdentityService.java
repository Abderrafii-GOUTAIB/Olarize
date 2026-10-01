package com.olarize.identity.application.service;

import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.identity.application.port.in.EmployeeLinkIdentityUseCase;
import com.olarize.identity.application.port.out.EmployeeLinkMembershipPort;
import com.olarize.identity.application.port.out.IdentitySubjectVerificationPort;
import com.olarize.identity.domain.exception.IdentityProviderUnavailable;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmployeeLinkIdentityService implements EmployeeLinkIdentityUseCase {
    private final CompanyAccessUseCase access;
    private final EmployeeLinkMembershipPort memberships;
    private final IdentitySubjectVerificationPort subjects;
    public EmployeeLinkIdentityService(CompanyAccessUseCase access, EmployeeLinkMembershipPort memberships,
            IdentitySubjectVerificationPort subjects) {
        this.access = access;
        this.memberships = memberships;
        this.subjects = subjects;
    }
    @Override public Target inspect(UUID companyId, UUID membershipId) {
        access.require(companyId, "identity.link.admin");
        return memberships.find(companyId, membershipId, false).orElse(null);
    }
    @Override public Target lock(UUID companyId, UUID membershipId) {
        // Called within the caller's signed mutation transaction; no REQUIRES_NEW lookup.
        return memberships.find(companyId, membershipId, true).orElse(null);
    }
    @Override public boolean verifyAccount(Target target) {
        if (target == null) throw new IllegalArgumentException("Missing verified membership");
        try {
            return subjects.exists(target.subject());
        } catch (IdentityProviderUnavailable unavailable) { throw new AccountVerificationUnavailable(); }
    }
}

package com.olarize.identity.application.port.out;

import com.olarize.identity.application.port.in.EmployeeLinkIdentityUseCase.Target;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeLinkMembershipPort {
    Optional<Target> find(UUID companyId, UUID membershipId, boolean lock);
}

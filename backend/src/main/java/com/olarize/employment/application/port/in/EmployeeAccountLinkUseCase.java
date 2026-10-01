package com.olarize.employment.application.port.in;

import com.olarize.employment.domain.model.EmployeeAccountLinkChange;
import com.olarize.employment.domain.model.EmployeeAccountLinkStatus;
import java.time.Instant;
import java.util.UUID;

public interface EmployeeAccountLinkUseCase {
    Result read(UUID companyId, UUID membershipId);
    Result change(UUID companyId, UUID membershipId, EmployeeAccountLinkChange change, UUID key, String correlationId);
    OwnLink own(UUID companyId);
    record Result(UUID companyId, UUID membershipId, UUID employeeId, EmployeeAccountLinkStatus status,
            long version, Instant recordedAt) { }
    record OwnLink(UUID companyId, UUID employeeId) { }
}

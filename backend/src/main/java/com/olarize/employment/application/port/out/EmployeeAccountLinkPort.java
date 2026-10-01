package com.olarize.employment.application.port.out;

import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase.Result;
import com.olarize.employment.domain.model.EmployeeAccountLinkChange;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAccountLinkPort {
    Optional<Result> find(UUID companyId, UUID membershipId);
    void serialize(UUID companyId, String actor, UUID key);
    Optional<Receipt> retry(UUID companyId, String actor, UUID key);
    boolean employeeExists(UUID companyId, UUID employeeId);
    Result mutate(UUID companyId, UUID membershipId, EmployeeAccountLinkChange change, String actor,
            UUID key, String hash, String correlationId, Instant recordedAt);
    record Receipt(String hash, Result result) { }
}

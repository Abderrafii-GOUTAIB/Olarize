package com.olarize.identity.application.port.in;

import java.util.List;
import java.util.UUID;

/** Always uses the authenticated subject, never an external subject argument. */
public interface CompanyAccessUseCase {
    List<CompanyContext> memberships();
    CompanyContext resolve(UUID requestedCompany);
}

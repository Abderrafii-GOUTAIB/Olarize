package com.olarize.identity.application.service;

import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.identity.application.port.in.CompanyContext;
import com.olarize.identity.application.port.out.AuthenticatedSubjectPort;
import com.olarize.identity.application.port.out.MembershipReaderPort;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.model.Membership;
import com.olarize.organization.application.port.in.CompanyLookupPort;
import com.olarize.organization.application.port.in.CompanyReadAuthorizationPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CurrentCompanyAccessService implements CompanyAccessUseCase, CompanyReadAuthorizationPort {
    private final AuthenticatedSubjectPort subject;
    private final MembershipReaderPort memberships;
    private final CompanyLookupPort companies;

    public CurrentCompanyAccessService(AuthenticatedSubjectPort subject, MembershipReaderPort memberships, CompanyLookupPort companies) {
        this.subject = subject;
        this.memberships = memberships;
        this.companies = companies;
    }
    @Override public List<CompanyContext> memberships() {
        return memberships.activeFor(subject.require()).stream().filter(Membership::active).map(this::context).toList();
    }
    @Override public CompanyContext resolve(UUID requestedCompany) {
        return context(Membership.select(memberships.activeFor(subject.require()), requestedCompany));
    }
    @Override public String currentUserId() { return subject.require(); }
    @Override public CompanyContext active(UUID requestedCompany) {
        try { return resolve(requestedCompany); }
        catch (CompanyAccessDenied denied) { throw new org.springframework.security.access.AccessDeniedException("Company access denied"); }
    }
    @Override public CompanyContext require(UUID requestedCompany, String permission) {
        try {
            CompanyContext selected = resolve(requestedCompany);
            if (selected.permits(permission)) return selected;
        } catch (CompanyAccessDenied denied) {
            // Preserve the same externally visible authorization outcome.
        }
        throw new org.springframework.security.access.AccessDeniedException("Company permission denied");
    }
    @Override public Optional<String> verifiedSubjectFor(UUID companyId) {
        if (companyId == null) return Optional.empty();
        String currentSubject = subject.require();
        return memberships.activeFor(currentSubject).stream()
                .anyMatch(membership -> membership.active() && membership.companyId().equals(companyId))
                ? Optional.of(currentSubject) : Optional.empty();
    }
    private CompanyContext context(Membership membership) {
        var company = companies.find(membership.companyId()).orElseThrow(com.olarize.identity.domain.exception.CompanyAccessDenied::new);
        return new CompanyContext(membership.id(), membership.companyId(), company.name(), membership.permissions(), membership.version());
    }
}

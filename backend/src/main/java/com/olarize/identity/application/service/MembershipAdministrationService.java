package com.olarize.identity.application.service;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.application.port.in.MembershipAdministrationUseCase;
import com.olarize.identity.application.port.out.AuthenticatedSubjectPort;
import com.olarize.identity.application.port.out.IdentitySubjectVerificationPort;
import com.olarize.identity.application.port.out.MembershipAdministrationPort;
import com.olarize.identity.application.port.out.MembershipPreviewTokenPort;
import com.olarize.identity.application.port.out.MembershipReaderPort;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.IdentityProviderUnavailable;
import com.olarize.identity.domain.exception.InvalidMembershipPreview;
import com.olarize.identity.domain.exception.MembershipChangeRejected;
import com.olarize.identity.domain.model.Membership;
import com.olarize.identity.domain.model.MembershipAdministrationRules;
import com.olarize.identity.domain.model.MembershipChangeAction;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MembershipAdministrationService implements MembershipAdministrationUseCase {
    private final AuthenticatedSubjectPort authenticatedSubject;
    private final MembershipReaderPort membershipReader;
    private final MembershipAdministrationPort administration;
    private final IdentitySubjectVerificationPort subjectVerification;
    private final MembershipPreviewTokenPort previewTokens;

    public MembershipAdministrationService(AuthenticatedSubjectPort authenticatedSubject,
            MembershipReaderPort membershipReader, MembershipAdministrationPort administration,
            IdentitySubjectVerificationPort subjectVerification, MembershipPreviewTokenPort previewTokens) {
        this.authenticatedSubject = authenticatedSubject;
        this.membershipReader = membershipReader;
        this.administration = administration;
        this.subjectVerification = subjectVerification;
        this.previewTokens = previewTokens;
    }

    @Override public List<Membership> list(UUID companyId) {
        String actor = authenticatedSubject.require();
        requireAdministrator(actor, companyId);
        return administration.list(actor, companyId);
    }

    @Override public Preview preview(UUID companyId, String targetSubject, MembershipRoleBundle roleBundle, String reason) {
        String actor = authenticatedSubject.require();
        Membership admin = requireAdministrator(actor, companyId);
        validateReason(reason);
        List<Membership> currentMembers = administration.list(actor, companyId);
        Membership current = currentMembers.stream().filter(member -> targetSubject.equals(member.subject())).findFirst().orElse(null);
        MembershipChangeAction action = MembershipAdministrationRules.action(actor, targetSubject, current, roleBundle);
        Set<String> permissions = MembershipAdministrationRules.permissions(action, roleBundle);
        verifySubjectIfNeeded(action, targetSubject);
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
        var claims = new MembershipChangePreviewClaims(UUID.randomUUID(), actor, companyId, targetSubject, action,
                roleBundle, permissions, admin.version(), current == null ? 0 : current.version(), reason.trim(), expiresAt);
        return new Preview(previewTokens.issue(claims), claims.previewId(), companyId, targetSubject, action.name(),
                roleBundle, permissions, claims.actorVersion(), claims.targetVersion(), expiresAt);
    }

    @Override public Membership confirm(UUID companyId, String token, long expectedTargetVersion,
            UUID idempotencyKey, String correlationId) {
        String actor = authenticatedSubject.require();
        MembershipChangePreviewClaims claims = previewTokens.verify(token);
        if (!claims.actorSubject().equals(actor) || !claims.companyId().equals(companyId)
                || claims.targetVersion() != expectedTargetVersion) throw new InvalidMembershipPreview();
        Membership admin = requireAdministrator(actor, companyId);
        if (admin.version() != claims.actorVersion()) throw new InvalidMembershipPreview();
        if (!MembershipAdministrationRules.permissions(claims.action(), claims.roleBundle()).equals(claims.permissions())) {
            throw new InvalidMembershipPreview();
        }
        if (idempotencyKey == null || correlationId == null || correlationId.isBlank()) throw new MembershipChangeRejected();
        // The database function checks current target state and idempotency under locks. This lets an exact
        // retry return its saved result even though the first confirmation already advanced the target version.
        Membership committedRetry = administration.findCommittedRetry(actor, claims, idempotencyKey);
        if (committedRetry != null) return committedRetry;
        verifySubjectIfNeeded(claims.action(), claims.targetSubject());
        return administration.change(actor, claims, idempotencyKey, correlationId);
    }

    private Membership requireAdministrator(String actor, UUID companyId) {
        if (companyId == null) throw new CompanyAccessDenied();
        return membershipReader.activeFor(actor).stream()
                .filter(member -> member.companyId().equals(companyId) && member.permissions().contains("identity.admin"))
                .findFirst().orElseThrow(CompanyAccessDenied::new);
    }

    private void verifySubjectIfNeeded(MembershipChangeAction action, String targetSubject) {
        if (!MembershipAdministrationRules.requiresVerifiedSubject(action)) return;
        final boolean exists;
        try { exists = subjectVerification.exists(targetSubject); }
        catch (IdentityProviderUnavailable unavailable) { throw unavailable; }
        catch (RuntimeException unavailable) { throw new IdentityProviderUnavailable(); }
        if (!exists) throw new MembershipChangeRejected();
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000) throw new MembershipChangeRejected();
    }
}

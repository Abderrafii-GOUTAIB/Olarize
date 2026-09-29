package com.olarize.identity.adapter.out.security;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.domain.exception.InvalidMembershipPreview;
import com.olarize.identity.domain.model.MembershipChangeAction;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MembershipPreviewTokenSignerTests {
    private final MembershipPreviewTokenSigner signer = new MembershipPreviewTokenSigner("0123456789abcdef0123456789abcdef");

    @Test void roundTripsExactPreviewClaims() {
        var claims = new MembershipChangePreviewClaims(UUID.randomUUID(), "actor", UUID.randomUUID(), "target",
                MembershipChangeAction.UPDATE, MembershipRoleBundle.HR_ADMIN, MembershipRoleBundle.HR_ADMIN.permissions(),
                7, 3, "reviewed role change", Instant.now().plusSeconds(600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        assertThat(signer.verify(signer.issue(claims))).isEqualTo(claims);
    }

    @Test void rejectsTamperedAndExpiredPreviews() {
        var claims = new MembershipChangePreviewClaims(UUID.randomUUID(), "actor", UUID.randomUUID(), "target",
                MembershipChangeAction.GRANT, MembershipRoleBundle.EMPLOYEE, Set.of("employee.self.read"),
                1, 0, "grant", Instant.now().plusSeconds(600));
        String token = signer.issue(claims);
        assertThatThrownBy(() -> signer.verify(token.substring(0, token.length() - 2) + "aa"))
                .isInstanceOf(InvalidMembershipPreview.class);
        var expired = new MembershipChangePreviewClaims(UUID.randomUUID(), "actor", UUID.randomUUID(), "target",
                MembershipChangeAction.GRANT, MembershipRoleBundle.EMPLOYEE, Set.of("employee.self.read"),
                1, 0, "grant", Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> signer.verify(signer.issue(expired))).isInstanceOf(InvalidMembershipPreview.class);
    }
}

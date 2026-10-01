package com.olarize.employment.domain.model;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmployeeAccountLinkChangeTests {
    private EmployeeAccountLinkChange change(EmployeeAccountLinkAction action, UUID employee, long version) {
        return new EmployeeAccountLinkChange(action, employee, version, 1, " Reviewed source ", " Verified evidence ");
    }
    @Test void requiresExplicitTransitionsAndDifferentReplacementTarget() {
        UUID first = UUID.randomUUID(); UUID second = UUID.randomUUID();
        assertThat(change(EmployeeAccountLinkAction.LINK, first, 0).permits(EmployeeAccountLinkStatus.UNLINKED, null)).isTrue();
        assertThat(change(EmployeeAccountLinkAction.LINK, first, 1).permits(EmployeeAccountLinkStatus.LINKED, first)).isFalse();
        assertThat(change(EmployeeAccountLinkAction.RELINK, second, 1).permits(EmployeeAccountLinkStatus.LINKED, first)).isTrue();
        assertThat(change(EmployeeAccountLinkAction.RELINK, first, 1).permits(EmployeeAccountLinkStatus.LINKED, first)).isFalse();
        assertThat(change(EmployeeAccountLinkAction.REVOKE, null, 1).permits(EmployeeAccountLinkStatus.LINKED, first)).isTrue();
        assertThat(change(EmployeeAccountLinkAction.REVOKE, null, 1).permits(EmployeeAccountLinkStatus.REVOKED, first)).isFalse();
        assertThat(change(EmployeeAccountLinkAction.LINK, second, 2).permits(EmployeeAccountLinkStatus.REVOKED, first)).isTrue();
    }
    @Test void validatesOperationTargetsVersionsAndEvidenceWithoutTruncation() {
        for (EmployeeAccountLinkAction action : new EmployeeAccountLinkAction[]{EmployeeAccountLinkAction.LINK, EmployeeAccountLinkAction.RELINK}) {
            assertThatThrownBy(() -> change(action, null, 0)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> change(EmployeeAccountLinkAction.REVOKE, UUID.randomUUID(), 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> change(EmployeeAccountLinkAction.LINK, UUID.randomUUID(), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeAccountLinkChange(EmployeeAccountLinkAction.LINK, UUID.randomUUID(), 0, 0, "Reason", "Evidence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeAccountLinkChange(EmployeeAccountLinkAction.LINK, UUID.randomUUID(), 0, 1, " ", "Evidence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmployeeAccountLinkChange(EmployeeAccountLinkAction.LINK, UUID.randomUUID(), 0, 1, "Reason", "x".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
        var normalized = change(EmployeeAccountLinkAction.LINK, UUID.randomUUID(), 0);
        assertThat(normalized.reason()).isEqualTo("Reviewed source");
        assertThat(normalized.verificationEvidenceReference()).isEqualTo("Verified evidence");
    }
}

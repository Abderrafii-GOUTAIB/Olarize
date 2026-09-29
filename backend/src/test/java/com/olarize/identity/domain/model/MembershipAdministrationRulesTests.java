package com.olarize.identity.domain.model;

import com.olarize.identity.domain.exception.MembershipChangeRejected;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MembershipAdministrationRulesTests {
    private Membership membership(Set<String> permissions, String status) {
        return new Membership(UUID.randomUUID(), UUID.randomUUID(), permissions, status, 3);
    }

    @Test void assignsOnlyFixedVersionedPermissionBundles() {
        assertThat(MembershipRoleBundle.HR_ADMIN.permissionVersion()).isEqualTo("v1");
        assertThat(MembershipRoleBundle.HR_ADMIN.permissions()).contains("employee.read", "contract.write")
                .doesNotContain("payroll.read", "payroll.roster.read", "identity.admin");
        assertThat(MembershipRoleBundle.MANAGER.permissions()).containsExactly("employee.direct-reports.read");
        assertThat(MembershipRoleBundle.EMPLOYEE.permissions()).containsExactly("employee.self.read");
    }

    @Test void distinguishesGrantUpdateReactivationAndRevocation() {
        assertThat(MembershipAdministrationRules.action("admin", "target", null, MembershipRoleBundle.EMPLOYEE))
                .isEqualTo(MembershipChangeAction.GRANT);
        var active = membership(MembershipRoleBundle.EMPLOYEE.permissions(), "ACTIVE");
        assertThat(MembershipAdministrationRules.action("admin", "target", active, MembershipRoleBundle.MANAGER))
                .isEqualTo(MembershipChangeAction.UPDATE);
        assertThat(MembershipAdministrationRules.action("admin", "target", membership(Set.of("old.permission"), "REVOKED"), MembershipRoleBundle.EMPLOYEE))
                .isEqualTo(MembershipChangeAction.REACTIVATE);
        assertThat(MembershipAdministrationRules.action("admin", "target", active, null))
                .isEqualTo(MembershipChangeAction.REVOKE);
    }

    @Test void rejectsSelfChangesProtectedAdminAndNoOpOrInvalidTransitions() {
        assertThatThrownBy(() -> MembershipAdministrationRules.action("same", "same", null, MembershipRoleBundle.EMPLOYEE))
                .isInstanceOf(MembershipChangeRejected.class);
        assertThatThrownBy(() -> MembershipAdministrationRules.action("admin", "target",
                membership(Set.of("identity.admin"), "ACTIVE"), null)).isInstanceOf(MembershipChangeRejected.class);
        var employee = membership(MembershipRoleBundle.EMPLOYEE.permissions(), "ACTIVE");
        assertThatThrownBy(() -> MembershipAdministrationRules.action("admin", "target", employee, MembershipRoleBundle.EMPLOYEE))
                .isInstanceOf(MembershipChangeRejected.class);
        assertThatThrownBy(() -> MembershipAdministrationRules.action("admin", "target", null, null))
                .isInstanceOf(MembershipChangeRejected.class);
        assertThatThrownBy(() -> MembershipAdministrationRules.action("admin", "target", membership(Set.of(), "REVOKED"), null))
                .isInstanceOf(MembershipChangeRejected.class);
    }

    @Test void onlyNewAndReactivatedMembershipsNeedProviderVerification() {
        assertThat(MembershipAdministrationRules.requiresVerifiedSubject(MembershipChangeAction.GRANT)).isTrue();
        assertThat(MembershipAdministrationRules.requiresVerifiedSubject(MembershipChangeAction.REACTIVATE)).isTrue();
        assertThat(MembershipAdministrationRules.requiresVerifiedSubject(MembershipChangeAction.UPDATE)).isFalse();
        assertThat(MembershipAdministrationRules.requiresVerifiedSubject(MembershipChangeAction.REVOKE)).isFalse();
    }
}

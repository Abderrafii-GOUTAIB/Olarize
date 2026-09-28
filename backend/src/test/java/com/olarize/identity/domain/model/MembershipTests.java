package com.olarize.identity.domain.model;

import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.CompanySelectionRequired;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MembershipTests {
    Membership member(String status) { return new Membership(UUID.randomUUID(), UUID.randomUUID(), Set.of("identity.admin"), status, 1); }
    @Test void singleActiveMembershipIsSelectedAndRevokedMembershipsAreExcluded() {
        var active = member("ACTIVE");
        var revoked = member("REVOKED");
        assertThat(Membership.select(List.of(active, revoked), null)).isEqualTo(active);
        assertThatThrownBy(() -> Membership.select(List.of(active, revoked), revoked.companyId())).isInstanceOf(CompanyAccessDenied.class);
        assertThatThrownBy(() -> Membership.select(List.of(revoked), null)).isInstanceOf(CompanyAccessDenied.class);
    }
    @Test void multipleMembershipsRequireExplicitSelectionAndDoNotCombinePermissions() {
        var a = member("ACTIVE");
        var b = new Membership(UUID.randomUUID(), UUID.randomUUID(), Set.of("employee.read"), "ACTIVE", 4);
        assertThatThrownBy(() -> Membership.select(List.of(a, b), null)).isInstanceOf(CompanySelectionRequired.class);
        assertThat(Membership.select(List.of(a, b), b.companyId())).isEqualTo(b);
        assertThatThrownBy(() -> Membership.select(List.of(a, b), UUID.randomUUID())).isInstanceOf(CompanyAccessDenied.class);
    }
}

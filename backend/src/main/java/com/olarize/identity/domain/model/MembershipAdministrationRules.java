package com.olarize.identity.domain.model;

import com.olarize.identity.domain.exception.MembershipChangeRejected;
import java.util.Set;

public final class MembershipAdministrationRules {
    private MembershipAdministrationRules() { }

    public static MembershipChangeAction action(String actor, String target, Membership current,
            MembershipRoleBundle requestedBundle) {
        if (actor == null || target == null || actor.equals(target) || target.isBlank() || target.length() > 255) {
            throw new MembershipChangeRejected();
        }
        if (current != null && current.permissions().contains("identity.admin")) {
            throw new MembershipChangeRejected();
        }
        if (requestedBundle == null) {
            if (current == null || !current.active()) throw new MembershipChangeRejected();
            return MembershipChangeAction.REVOKE;
        }
        if (current == null) return MembershipChangeAction.GRANT;
        if (current.active()) {
            if (requestedBundle.permissions().equals(current.permissions())) throw new MembershipChangeRejected();
            return MembershipChangeAction.UPDATE;
        }
        return MembershipChangeAction.REACTIVATE;
    }

    public static boolean requiresVerifiedSubject(MembershipChangeAction action) {
        return action == MembershipChangeAction.GRANT || action == MembershipChangeAction.REACTIVATE;
    }

    public static Set<String> permissions(MembershipChangeAction action, MembershipRoleBundle bundle) {
        if (action == MembershipChangeAction.REVOKE) {
            if (bundle != null) throw new MembershipChangeRejected();
            return Set.of();
        }
        if (bundle == null) throw new MembershipChangeRejected();
        return bundle.permissions();
    }
}

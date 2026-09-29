package com.olarize.identity.domain.model;

import java.util.Set;

/** Versioned, fixed permission bundles assignable by company identity administrators. */
public enum MembershipRoleBundle {
    HR_ADMIN("v1", Set.of("employee.read", "employee.write", "contract.read", "contract.write", "employee.import")),
    MANAGER("v1", Set.of("employee.direct-reports.read")),
    EMPLOYEE("v1", Set.of("employee.self.read")),
    PAYROLL_OPERATOR("v1", Set.of("payroll.roster.read"));

    private final String permissionVersion;
    private final Set<String> permissions;

    MembershipRoleBundle(String permissionVersion, Set<String> permissions) {
        this.permissionVersion = permissionVersion;
        this.permissions = Set.copyOf(permissions);
    }

    public String permissionVersion() { return permissionVersion; }
    public Set<String> permissions() { return permissions; }
}

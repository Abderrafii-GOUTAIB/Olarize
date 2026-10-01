package com.olarize.employment.domain.model;

import java.util.UUID;

public record EmployeeAccountLinkChange(EmployeeAccountLinkAction action, UUID employeeId,
        long expectedVersion, long expectedMembershipVersion, String reason, String verificationEvidenceReference) {
    public EmployeeAccountLinkChange {
        if (action == null || expectedVersion < 0 || expectedMembershipVersion < 1
                || (action == EmployeeAccountLinkAction.REVOKE ? employeeId != null : employeeId == null)
                || reason == null || reason.isBlank() || reason.trim().length() > 1000
                || verificationEvidenceReference == null || verificationEvidenceReference.isBlank()
                || verificationEvidenceReference.trim().length() > 200) {
            throw new IllegalArgumentException("Invalid link change");
        }
        reason = reason.trim();
        verificationEvidenceReference = verificationEvidenceReference.trim();
    }
    public boolean permits(EmployeeAccountLinkStatus status, UUID previousEmployee) {
        return switch (action) {
            case LINK -> status != EmployeeAccountLinkStatus.LINKED;
            case RELINK -> status == EmployeeAccountLinkStatus.LINKED && !employeeId.equals(previousEmployee);
            case REVOKE -> status == EmployeeAccountLinkStatus.LINKED;
        };
    }
}

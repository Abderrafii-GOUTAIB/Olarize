package com.olarize.identity.domain.exception;

public final class MembershipChangeRejected extends RuntimeException {
    public MembershipChangeRejected() { super("Membership change is not allowed"); }
}

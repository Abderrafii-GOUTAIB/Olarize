package com.olarize.identity.domain.exception;

public final class InvalidMembershipPreview extends RuntimeException {
    public InvalidMembershipPreview() { super("Membership preview is invalid or expired"); }
}

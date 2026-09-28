package com.olarize.organization.internal.domain;

public final class RegistrationConflict extends RuntimeException {
    public RegistrationConflict() { super("Idempotency conflict"); }
}

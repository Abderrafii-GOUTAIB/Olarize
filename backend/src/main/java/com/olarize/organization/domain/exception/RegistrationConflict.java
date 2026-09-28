package com.olarize.organization.domain.exception;

public final class RegistrationConflict extends RuntimeException {
    public RegistrationConflict() { super("Idempotency conflict"); }
}

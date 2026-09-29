package com.olarize.identity.domain.exception;

public final class IdentityProviderUnavailable extends RuntimeException {
    public IdentityProviderUnavailable() { super("Identity provider is unavailable"); }
}

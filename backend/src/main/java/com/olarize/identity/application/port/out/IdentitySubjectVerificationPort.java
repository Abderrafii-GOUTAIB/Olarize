package com.olarize.identity.application.port.out;

public interface IdentitySubjectVerificationPort {
    boolean exists(String subject);
}

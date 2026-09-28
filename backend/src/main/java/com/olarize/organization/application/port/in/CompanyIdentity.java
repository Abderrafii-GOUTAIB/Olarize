package com.olarize.organization.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record CompanyIdentity(UUID id, String name, long version, Instant createdAt, Instant updatedAt) { }

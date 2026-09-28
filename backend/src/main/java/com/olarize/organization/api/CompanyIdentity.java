package com.olarize.organization.api;

import java.time.Instant;
import java.util.UUID;

public record CompanyIdentity(UUID id, String name, long version, Instant createdAt, Instant updatedAt) { }

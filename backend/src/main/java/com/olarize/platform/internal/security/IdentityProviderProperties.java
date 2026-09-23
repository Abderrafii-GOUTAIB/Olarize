package com.olarize.platform.internal.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("olarize.auth")
record IdentityProviderProperties(@NotBlank String issuer, @NotBlank String jwkSetUri,
        @NotBlank String audience, @NotBlank String clientId) { }

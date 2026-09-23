package com.olarize.platform.internal.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProviderProperties.class)
class IdentityProviderConfiguration {
    @Bean
    JwtDecoder jwtDecoder(IdentityProviderProperties properties) {
        // Separate transport URL allows Docker to fetch keys internally while validating the public issuer.
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> accessToken = jwt -> {
            boolean valid = "Bearer".equals(jwt.getClaimAsString("typ"))
                    && properties.clientId().equals(jwt.getClaimAsString("azp"))
                    && jwt.getAudience().contains(properties.audience())
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                    && jwt.getExpiresAt().isAfter(jwt.getIssuedAt());
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), accessToken));
        return decoder;
    }
}

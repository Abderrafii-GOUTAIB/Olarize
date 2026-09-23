package com.olarize.platform.internal.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalOpenApiConfiguration {
    @Bean
    OpenAPI olarizeApi(@Value("${olarize.auth.issuer}") String issuer) {
        var authorization = new OAuthFlow()
            .authorizationUrl(issuer + "/protocol/openid-connect/auth")
            .tokenUrl(issuer + "/protocol/openid-connect/token")
            .scopes(new Scopes().addString("profile.read", "Read the authenticated subject"));
        return new OpenAPI()
            .info(new Info().title("Olarize API").version("0.1.0")
                .description("Local API reference. HR data requires application permissions."))
            .components(new Components().addSecuritySchemes("keycloak",
                new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows().authorizationCode(authorization))));
    }
}

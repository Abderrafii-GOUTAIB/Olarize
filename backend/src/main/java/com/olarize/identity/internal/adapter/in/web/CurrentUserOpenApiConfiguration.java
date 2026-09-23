package com.olarize.identity.internal.adapter.in.web;

import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class CurrentUserOpenApiConfiguration {
    @Bean
    OpenApiCustomizer currentUserDocumentation() {
        return api -> {
            var operation = api.getPaths().get("/api/v1/me").getGet();
            operation.setSummary("Return the authenticated external subject");
            operation.setDescription("Requires a Keycloak access token with profile.read. This does not grant HR access.");
            operation.addSecurityItem(new SecurityRequirement().addList("keycloak", "profile.read"));
            operation.getResponses().computeIfAbsent("200", code -> new ApiResponse())
                .setDescription("The authenticated subject");
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid access token"));
            operation.getResponses().addApiResponse("403", new ApiResponse().description("Missing profile.read scope"));
        };
    }
}

package com.olarize.identity.adapter.in.web;

import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class CurrentUserOpenApiConfiguration {
    private static void secure(io.swagger.v3.oas.models.Operation operation) {
        operation.addSecurityItem(new SecurityRequirement().addList("keycloak", "profile.read"));
        operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid Keycloak access token"));
    }

    private static void documentMemberships(io.swagger.v3.oas.models.OpenAPI api) {
        var item = api.getPaths().get("/api/v1/identity/memberships");
        if (item == null || item.getGet() == null) return;
        var operation = item.getGet();
        operation.setSummary("List my active company memberships");
        operation.setDescription("Returns the companies that the signed-in user is currently an active member of. "
            + "Each item includes the company ID and name, membership ID, current permissions, and membership version. "
            + "The user is identified from the validated Keycloak access token; no user ID or company ID is accepted "
            + "as proof of membership. If the user has no active memberships, this endpoint returns an empty list. "
            + "Memberships are loaded from current database state on every request, so changes and revocations are reflected "
            + "on the next request. Requires the profile.read scope.");
        secure(operation);
        operation.getResponses().computeIfAbsent("200", code -> new ApiResponse())
            .setDescription("The signed-in user's active company memberships; an empty array means there are none.");
        operation.getResponses().addApiResponse("403", new ApiResponse().description("The access token is valid but does not include profile.read"));
        operation.getResponses().addApiResponse("400", new ApiResponse().description("Unsupported query parameters were supplied"));
    }

    private static void documentCompanyContext(io.swagger.v3.oas.models.OpenAPI api) {
        var item = api.getPaths().get("/api/v1/identity/company-context");
        if (item == null || item.getGet() == null) return;
        var operation = item.getGet();
        operation.setSummary("Resolve my active company context");
        operation.setDescription("Returns the company context for one of the signed-in user's current active memberships. "
            + "Without companyId, the endpoint selects the company when the user has exactly one active membership. "
            + "If the user has several active memberships, pass one of the companyId values returned by "
            + "GET /api/v1/identity/memberships. The response includes that membership's current permissions and version. "
            + "A requested company ID does not grant access; membership is checked against current database state "
            + "on every request. Requires the profile.read scope.");
        secure(operation);
        operation.getResponses().computeIfAbsent("200", code -> new ApiResponse())
            .setDescription("The selected company and the signed-in user's active membership permissions.");
        operation.getResponses().addApiResponse("400", new ApiResponse().description("The companyId is malformed or duplicated, an unsupported query parameter was supplied, or companyId is required because the user has multiple active memberships"));
        operation.getResponses().addApiResponse("403", new ApiResponse().description("The user has no active membership in the requested company, or no active membership can be selected automatically"));
        operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid Keycloak access token"));
    }

    private static void documentCurrentUser(io.swagger.v3.oas.models.OpenAPI api) {
        var item = api.getPaths().get("/api/v1/identity");
        if (item == null || item.getGet() == null) return;
        var operation = item.getGet();
        operation.setSummary("Return the current user ID");
        operation.setDescription("Returns currentUserId from the validated Keycloak access token. "
            + "This identifies the signed-in person and does not grant access to company or HR data. "
            + "Requires the profile.read scope.");
        secure(operation);
        operation.getResponses().computeIfAbsent("200", code -> new ApiResponse())
            .setDescription("The signed-in person's Keycloak user ID from the access token.");
        operation.getResponses().addApiResponse("403", new ApiResponse().description("The access token is valid but does not include profile.read"));
    }

    private static void secureMembershipAdministration(io.swagger.v3.oas.models.OpenAPI api) {
        secure(api, "/api/v1/companies/{companyId}/memberships", false);
        secure(api, "/api/v1/companies/{companyId}/membership-change-previews", true);
        secure(api, "/api/v1/companies/{companyId}/membership-changes/confirm", true);
    }

    private static void secure(io.swagger.v3.oas.models.OpenAPI api, String path, boolean post) {
        var pathItem = api.getPaths().get(path);
        if (pathItem == null) return;
        var operation = post ? pathItem.getPost() : pathItem.getGet();
        if (operation != null) secure(operation);
    }

    @Bean
    OpenApiCustomizer currentUserDocumentation() {
        return api -> {
            documentCurrentUser(api);
            documentMemberships(api);
            documentCompanyContext(api);
            secureMembershipAdministration(api);
        };
    }
}

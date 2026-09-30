package com.olarize.identity.infrastructure.in.web.configuration;


import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Examples match the initial persistent local demo seed; they never write data. */
@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalIdentityExamplesConfiguration {
    private static final String COMPANY = "c0000000-0000-4000-8000-000000000001";
    private static final String ADMIN = "c0000000-0000-4000-8000-000000000011";
    private static final String MEMBER = "c0000000-0000-4000-8000-000000000021";
    private static final String TOKEN = "COPY_REAL_PREVIEW_TOKEN_FROM_PREVIEW_RESPONSE";

    @Bean
    OpenApiCustomizer localIdentityExamples() {
        return api -> {
            var schemas = api.getComponents().getSchemas();
            schemas.get("CurrentUserResponseDto").setExample(Map.of("currentUserId", ADMIN));
            schemas.get("CompanyContextResponseDto").setExample(Map.of(
                    "membershipId", "c0000000-0000-4000-8000-000000000010",
                    "companyId", COMPANY, "companyName", "Demo Morocco SARL",
                    "permissions", List.of("identity.admin"), "version", 1));
            schemas.get("ManagedMembershipResponseDto").setExample(Map.of(
                    "membershipId", "c0000000-0000-4000-8000-000000000020",
                    "userId", MEMBER, "companyId", COMPANY, "status", "ACTIVE",
                    "roleBundle", "EMPLOYEE", "permissions", List.of("employee.self.read"), "version", 1));
            schemas.get("MembershipChangePreviewRequestDto").setExample(Map.of(
                    "userId", MEMBER, "roleBundle", "MANAGER",
                    "reason", "Assign manager access to the demo member."));
            var confirmation = schemas.get("ConfirmMembershipChangeRequestDto");
            confirmation.setDescription("Run the preview first. Copy its real previewToken and targetVersion. "
                    + "Generate a fresh UUID for each new change; reuse it only for an identical retry. "
                    + "The example token is an instruction, not a usable signed token.");
            confirmation.setExample(Map.of("previewToken", TOKEN, "expectedTargetVersion", 1,
                    "idempotencyKey", "78204b94-fb77-472e-a39f-524fe2d8c879", "confirm", true));
            var preview = schemas.get("MembershipPreviewResponseDto");
            preview.setExample(Map.ofEntries(
                    Map.entry("previewToken", TOKEN),
                    Map.entry("previewId", "c0000000-0000-4000-8000-000000000060"),
                    Map.entry("companyId", COMPANY), Map.entry("userId", MEMBER),
                    Map.entry("action", "UPDATE"), Map.entry("roleBundle", "MANAGER"),
                    Map.entry("permissions", List.of("employee.direct-reports.read")),
                    Map.entry("actorVersion", 1), Map.entry("targetVersion", 1),
                    Map.entry("expiresAt", "2026-09-29T12:30:00Z")));
            preview.setDescription("Illustrative response. Execute the preview to obtain a real token and expiry, "
                    + "valid for 30 minutes. Examples describe the initial seed; current versions may differ.");
            api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getParameters() != null) {
                    operation.getParameters().stream().filter(parameter -> "companyId".equals(parameter.getName()))
                            .forEach(parameter -> parameter.setExample(COMPANY));
                }
            }));
            api.getPaths().get("/api/v1/companies/{companyId}/membership-changes/confirm").getPost()
                    .getResponses().get("200").getContent().get("application/json").setExample(Map.of(
                            "membershipId", "c0000000-0000-4000-8000-000000000020",
                            "userId", MEMBER, "companyId", COMPANY, "status", "ACTIVE",
                            "roleBundle", "MANAGER", "permissions", List.of("employee.direct-reports.read"), "version", 2));
        };
    }
}

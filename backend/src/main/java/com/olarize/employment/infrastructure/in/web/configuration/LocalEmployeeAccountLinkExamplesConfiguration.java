package com.olarize.employment.infrastructure.in.web.configuration;

import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalEmployeeAccountLinkExamplesConfiguration {
    @Bean OpenApiCustomizer localEmployeeAccountLinkExamples() {
        return api -> {
            api.getComponents().getSchemas().get("ChangeEmployeeAccountLinkRequestDto").setExample(Map.of(
                    "action", "REVOKE", "expectedVersion", 1, "expectedMembershipVersion", 1,
                    "reason", "Reviewed synthetic link revocation",
                    "verificationEvidenceReference", "local-demo-reviewed-link",
                    "idempotencyKey", "c0000000-0000-4000-8000-000000000099", "confirm", true));
            for (String path : new String[]{"/api/v1/companies/{companyId}/employee-account-links/{membershipId}",
                    "/api/v1/companies/{companyId}/employee-account-links/{membershipId}/changes",
                    "/api/v1/companies/{companyId}/my-employee-link"}) {
                var item = api.getPaths().get(path);
                var operation = item.getGet() == null ? item.getPost() : item.getGet();
                for (var parameter : operation.getParameters()) {
                    String suffix = parameter.getName().equals("companyId") ? "001" : "020";
                    parameter.setExample("c0000000-0000-4000-8000-000000000" + suffix);
                }
            }
        };
    }
}

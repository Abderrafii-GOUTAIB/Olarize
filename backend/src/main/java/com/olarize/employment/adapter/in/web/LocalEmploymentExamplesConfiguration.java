package com.olarize.employment.adapter.in.web;

import java.util.Map;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalEmploymentExamplesConfiguration {
    @Bean
    OpenApiCustomizer localEmployeeExamples() {
        return api -> {
            api.getPaths().get("/api/v1/companies/{companyId}/employees").getPost()
                    .addSecurityItem(new SecurityRequirement().addList("keycloak", "profile.read"));
            api.getComponents().getSchemas().get("CreateEmployeeRequest").setExample(Map.ofEntries(
                    Map.entry("reference", "EMP-0007"),
                    Map.entry("name", "Salma El Idrissi"),
                    Map.entry("startDate", "2026-10-05"),
                    Map.entry("jobTitle", "People Operations Specialist"),
                    Map.entry("department", "People Operations"),
                    Map.entry("site", "Casablanca"),
                    Map.entry("workEmail", "salma@demo.olarize.invalid"),
                    Map.entry("workPhone", "+212 5 22 00 00 00"),
                    Map.entry("idempotencyKey", "dcb466cc-22bb-4a6e-90c1-5e3ff5743480"),
                    Map.entry("confirm", true)));
            api.getComponents().getSchemas().get("CreatedEmployeeResponse").setExample(Map.of(
                    "employeeId", "c0000000-0000-4000-8000-000000000070",
                    "episodeId", "c0000000-0000-4000-8000-000000000071",
                    "companyId", "c0000000-0000-4000-8000-000000000001",
                    "reference", "EMP-0007", "name", "Salma El Idrissi",
                    "startDate", "2026-10-05", "status", "UPCOMING", "version", 1));
        };
    }
}

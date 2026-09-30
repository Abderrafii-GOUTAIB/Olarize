package com.olarize.employment.adapter.in.web;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalContractExamplesConfiguration {
    private static final String CREATE_PATH = "/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts";
    private static final String CORRECT_PATH = CREATE_PATH + "/{contractId}/corrections";

    @Bean
    OpenApiCustomizer localContractExamples() {
        return api -> {
            var create = api.getPaths().get(CREATE_PATH).getPost();
            var correct = api.getPaths().get(CORRECT_PATH).getPost();
            create.addSecurityItem(new SecurityRequirement().addList("keycloak", "profile.read"));
            correct.addSecurityItem(new SecurityRequirement().addList("keycloak", "profile.read"));
            setPathExample(create.getParameters(), "companyId", "c0000000-0000-4000-8000-000000000001");
            setPathExample(create.getParameters(), "employeeId", "c0000000-0000-4000-8000-000000000070");
            setPathExample(create.getParameters(), "episodeId", "c0000000-0000-4000-8000-000000000071");
            setPathExample(correct.getParameters(), "companyId", "c0000000-0000-4000-8000-000000000001");
            setPathExample(correct.getParameters(), "employeeId", "c0000000-0000-4000-8000-000000000070");
            setPathExample(correct.getParameters(), "episodeId", "c0000000-0000-4000-8000-000000000071");
            setPathExample(correct.getParameters(), "contractId", "c0000000-0000-4000-8000-000000000073");
            api.getComponents().getSchemas().get("CreateContractRequest").setExample(Map.of(
                    "typeCode", "CDD", "startDate", "2026-09-01", "endDate", "2027-08-31",
                    "sourceReference", "Synthetic HR archive item DEMO-CONTRACT-0002",
                    "idempotencyKey", "108f9945-6aae-4f39-8ab5-8b11fdb99772", "confirm", true));
            api.getComponents().getSchemas().get("CorrectContractRequest").setExample(Map.of(
                    "expectedVersion", 1, "typeCode", "CDD", "startDate", "2026-09-01",
                    "endDate", "2027-12-31", "sourceReference", "Amended synthetic paper contract",
                    "reason", "The original end date was transcribed incorrectly.",
                    "evidenceReference", "HR archive item DEMO-CONTRACT-AMENDMENT-0001",
                    "idempotencyKey", "894f5ba8-e8f9-459d-921e-51605201fb08", "confirm", true));
            create.getResponses().get("201").getContent().get("application/json").getSchema()
                    .setExample(contractExample(1, "2027-08-31", "Synthetic HR archive item DEMO-CONTRACT-0002"));
            correct.getResponses().get("200").getContent().get("application/json").getSchema()
                    .setExample(contractExample(2, "2027-12-31", "Amended synthetic paper contract"));
        };
    }

    private static void setPathExample(java.util.List<Parameter> parameters, String name, String value) {
        parameters.stream().filter(parameter -> parameter.getName().equals(name))
                .findFirst().ifPresent(parameter -> parameter.setExample(value));
    }

    private static Map<String, Object> contractExample(long version, String endDate, String sourceReference) {
        return Map.ofEntries(Map.entry("contractId", "c0000000-0000-4000-8000-000000000073"),
                Map.entry("companyId", "c0000000-0000-4000-8000-000000000001"),
                Map.entry("employeeId", "c0000000-0000-4000-8000-000000000070"),
                Map.entry("episodeId", "c0000000-0000-4000-8000-000000000071"),
                Map.entry("version", version), Map.entry("typeCode", "CDD"),
                Map.entry("startDate", "2026-09-01"), Map.entry("endDate", endDate),
                Map.entry("endDateCompleteness", "RECORDED"),
                Map.entry("sourceReference", sourceReference), Map.entry("reviewState", "UNREVIEWED"),
                Map.entry("recordedAt", "2026-09-30T10:00:00Z"));
    }
}

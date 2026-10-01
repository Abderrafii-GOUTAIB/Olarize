package com.olarize.employment.infrastructure.in.web.configuration;

import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalEmployeeCorrectionExamplesConfiguration {
    @Bean OpenApiCustomizer localEmployeeCorrectionExamples() {
        return api -> {
            api.getComponents().getSchemas().get("CorrectEmployeeRequestDto").setExample(Map.ofEntries(
                    Map.entry("reference", "EMP-0001"), Map.entry("name", "Yasmine Bennani"),
                    Map.entry("startDate", "2026-09-01"), Map.entry("effectiveDate", "2026-10-01"),
                    Map.entry("jobTitle", "Senior HR Coordinator"), Map.entry("department", "People Operations"),
                    Map.entry("site", "Casablanca"), Map.entry("workEmail", "yasmine@demo.olarize.invalid"),
                    Map.entry("workPhone", "+212 5 22 00 00 01"), Map.entry("expectedVersion", 1),
                    Map.entry("expectedEpisodeVersion", 1), Map.entry("reason", "Reviewed synthetic job assignment"),
                    Map.entry("idempotencyKey", "c0000000-0000-4000-8000-000000000090"), Map.entry("confirm", true)));
            var operation = api.getPaths().get("/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/corrections").getPost();
            for (var parameter : operation.getParameters()) {
                String suffix = switch (parameter.getName()) {
                    case "companyId" -> "001";
                    case "employeeId" -> "070";
                    case "episodeId" -> "071";
                    default -> null;
                };
                if (suffix != null) parameter.setExample("c0000000-0000-4000-8000-000000000" + suffix);
            }
        };
    }
}

package com.olarize.assistant.infrastructure.in.web.configuration;

import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalContractExpiryAssistantExamplesConfiguration {
    @Bean OpenApiCustomizer localContractExpiryAssistantExamples() {
        return api -> {
            String company = "c0000000-0000-4000-8000-000000000001";
            var schemas = api.getComponents().getSchemas();
            schemas.get("ContractExpiryQuestionRequestDto").setExample(Map.of(
                    "question", "Which contracts expire in October 2026?", "page", 0, "pageSize", 25));
            var result = Map.ofEntries(Map.entry("companyId", company), Map.entry("fromDate", "2026-10-01"),
                    Map.entry("toDate", "2026-10-31"), Map.entry("asOf", "2026-09-30"), Map.entry("timeZone", "Africa/Casablanca"),
                    Map.entry("page", 0), Map.entry("pageSize", 25), Map.entry("totalItems", 1), Map.entry("totalPages", 1),
                    Map.entry("currentContractsWithoutRecordedEndDate", 0), Map.entry("items", List.of(Map.of(
                            "contractId", "c0000000-0000-4000-8000-000000000090", "contractVersion", 1,
                            "employeeId", "c0000000-0000-4000-8000-000000000070", "employeeName", "Yasmine Bennani",
                            "employeeReference", "EMP-0001", "startDate", "2026-09-01", "endDate", "2026-10-15"))));
            schemas.get("ExpiryResultResponseDto").setExample(result);
            for (String path : List.of("/api/v1/companies/{companyId}/assistant/conversations",
                    "/api/v1/companies/{companyId}/assistant/conversations/{conversationId}/contract-expiry-requests")) {
                api.getPaths().get(path).getPost().getParameters().stream()
                        .filter(parameter -> "companyId".equals(parameter.getName()))
                        .forEach(parameter -> parameter.setExample(company));
            }
        };
    }
}

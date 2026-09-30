package com.olarize.employment.infrastructure.in.web.configuration;

import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local & !dev & !prod")
class LocalEmployeeDirectoryExamplesConfiguration {
    private static final String COMPANY = "c0000000-0000-4000-8000-000000000001";

    @Bean
    OpenApiCustomizer localEmployeeDirectoryExamples() {
        return api -> {
            var schemas = api.getComponents().getSchemas();
            schemas.get("EmployeeDirectoryResponseDto").setExample(Map.of(
                    "companyId", COMPANY, "asOf", "2026-09-30", "timeZone", "Africa/Casablanca",
                    "items", List.of(summary("080", "081", "EMP-0002", "Accountant", "Finance", "Casablanca", "2026-01-01"),
                            summary("082", "083", "EMP-0003", "Operations Coordinator", "Operations", "Rabat", "2026-02-01")),
                    "pagination", Map.of("page", 0, "pageSize", 25, "totalItems", 2, "totalPages", 1)));
            schemas.get("EmployeeDetailResponseDto").setExample(Map.ofEntries(
                    Map.entry("employeeId", id("080")), Map.entry("companyId", COMPANY),
                    Map.entry("reference", "EMP-0002"), Map.entry("name", "Ahmed El Amrani"),
                    Map.entry("jobTitle", "Accountant"), Map.entry("department", "Finance"), Map.entry("site", "Casablanca"),
                    Map.entry("version", 1), Map.entry("recordedAt", "2026-09-30T00:00:00Z"),
                    Map.entry("episodeId", id("081")), Map.entry("startDate", "2026-01-01"),
                    Map.entry("episodeVersion", 1), Map.entry("status", "CURRENT"),
                    Map.entry("asOf", "2026-09-30"), Map.entry("timeZone", "Africa/Casablanca")));
            for (String path : List.of("/api/v1/companies/{companyId}/employees",
                    "/api/v1/companies/{companyId}/employees/{employeeId}")) {
                api.getPaths().get(path).getGet().getParameters().stream()
                        .filter(parameter -> "companyId".equals(parameter.getName()))
                        .forEach(parameter -> parameter.setExample(COMPANY));
            }
        };
    }

    private static Map<String, Object> summary(String employee, String episode, String reference,
            String jobTitle, String department, String site, String startDate) {
        return Map.ofEntries(Map.entry("employeeId", id(employee)), Map.entry("episodeId", id(episode)),
                Map.entry("reference", reference), Map.entry("name", "Ahmed El Amrani"),
                Map.entry("jobTitle", jobTitle), Map.entry("department", department), Map.entry("site", site),
                Map.entry("startDate", startDate), Map.entry("status", "CURRENT"),
                Map.entry("version", 1), Map.entry("episodeVersion", 1));
    }

    private static String id(String suffix) { return "c0000000-0000-4000-8000-000000000" + suffix; }
}

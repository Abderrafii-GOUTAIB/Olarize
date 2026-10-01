package com.olarize.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalDocumentationIntegrationTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        // The local profile configures Flyway separately from the application datasource.
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.flyway.placeholders.local_runtime_password", () -> "synthetic-local-runtime-only");
        properties.add("spring.flyway.placeholders.db_scope_key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.add("olarize.identity.database-scope-key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.olarize.platform.api.DatabaseScopeSigner scope;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test void localCorrectionSwaggerAndPersistentBaselinesAreAvailable() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        var operation = mapper.readTree(response.body()).path("paths")
                .path("/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/corrections").path("post");
        assertThat(operation.path("summary").asText()).contains("Correct employee");
        assertThat(operation.path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText())
                .contains("CorrectedEmployeeResponseDto");
        var example = mapper.readTree(response.body()).path("components").path("schemas").path("CorrectEmployeeRequestDto").path("example");
        assertThat(example.path("confirm").asBoolean()).isTrue();
        assertThat(example.path("expectedVersion").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from employment_employee_version where employee_id = 'c0000000-0000-4000-8000-000000000070' and version = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from employment_job_version where episode_id = 'c0000000-0000-4000-8000-000000000071' and job_title = 'HR Coordinator'", Integer.class)).isEqualTo(1);
    }

    @Test void localOpenApiListsOnlyTheEndpointAndBearerScheme() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("/api/v1/identity", "keycloak", "authorizationCode", "profile.read")
            .doesNotContain("/api/employees", "DB_PASSWORD", "sensitive-query-value");

        var operation = mapper.readTree(response.body()).path("paths").path("/api/v1/identity").path("get");
        assertThat(operation.path("summary").asText()).isEqualTo("Return the current user ID");
        assertThat(operation.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var successSchema = operation.path("responses").path("200").path("content")
            .path("application/json").path("schema");
        assertThat(successSchema.isObject()).isTrue();
        assertThat(successSchema.size()).isGreaterThan(0);
        assertThat(response.body()).contains("currentUserId");
        assertThat(operation.path("responses").path("401").path("description").asText()).isNotBlank();
        assertThat(operation.path("responses").path("403").path("description").asText()).isNotBlank();

        var memberships = mapper.readTree(response.body()).path("paths").path("/api/v1/identity/memberships").path("get");
        assertThat(memberships.path("summary").asText()).contains("active company memberships");
        assertThat(memberships.path("description").asText()).contains("empty list", "validated Keycloak access token", "current database state");
        assertThat(memberships.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var membershipItem = memberships.path("responses").path("200").path("content").path("application/json")
                .path("schema").path("items");
        assertThat(membershipItem.path("$ref").asText()).isEqualTo("#/components/schemas/CompanyContextResponseDto");
        assertThat(response.body()).contains("companyId", "companyName", "permissions");
        assertThat(memberships.path("responses").path("200").path("description").asText()).contains("empty array");
        assertThat(memberships.path("responses").path("403").path("description").asText()).contains("profile.read");
        var context = mapper.readTree(response.body()).path("paths").path("/api/v1/identity/company-context").path("get");
        assertThat(context.path("summary").asText()).contains("active company context");
        assertThat(context.path("description").asText()).contains("exactly one active membership", "pass one of the companyId", "does not grant access");
        assertThat(context.path("parameters").get(0).path("name").asText()).isEqualTo("companyId");
        assertThat(context.path("parameters").get(0).path("description").asText()).contains("Optional company UUID");
        assertThat(context.path("parameters").get(0).path("schema").path("format").asText()).isEqualTo("uuid");
        assertThat(context.path("responses").path("400").path("description").asText()).contains("multiple active memberships");
        assertThat(context.path("responses").path("403").path("description").asText())
                .contains("no active membership", "selected automatically");
        assertThat(response.body()).contains("companyName", "membershipId").doesNotContain("/api/v1/organizations");
        var schemas = mapper.readTree(response.body()).path("components").path("schemas");
        assertThat(schemas.path("CurrentUserResponseDto").path("properties").has("currentUserId")).isTrue();
        assertThat(schemas.path("ManagedMembershipResponseDto").path("properties").has("userId")).isTrue();
        assertThat(schemas.path("MembershipChangePreviewRequestDto").path("properties").has("userId")).isTrue();
        assertThat(schemas.path("MembershipPreviewResponseDto").path("properties").has("userId")).isTrue();
        var previewExample = schemas.path("MembershipChangePreviewRequestDto").path("example");
        assertThat(previewExample.path("userId").asText()).isEqualTo("c0000000-0000-4000-8000-000000000021");
        assertThat(previewExample.path("roleBundle").asText()).isEqualTo("MANAGER");
        assertThat(schemas.path("CompanyContextResponseDto").path("example").path("companyName").asText()).isEqualTo("Demo Morocco SARL");
        assertThat(schemas.path("ConfirmMembershipChangeRequestDto").path("example").path("previewToken").asText())
                .isEqualTo("COPY_REAL_PREVIEW_TOKEN_FROM_PREVIEW_RESPONSE");
        assertThat(context.path("parameters").get(0).path("example").asText()).isEqualTo("c0000000-0000-4000-8000-000000000001");

        var paths = mapper.readTree(response.body()).path("paths");
        var companyMemberships = paths.path("/api/v1/companies/{companyId}/memberships").path("get");
        var preview = paths.path("/api/v1/companies/{companyId}/membership-change-previews").path("post");
        var confirm = paths.path("/api/v1/companies/{companyId}/membership-changes/confirm").path("post");
        var createEmployee = paths.path("/api/v1/companies/{companyId}/employees").path("post");
        assertThat(createEmployee.path("description").asText()).contains("employee.write", "first episode");
        assertThat(createEmployee.path("security").get(0).path("keycloak").get(0).asText())
                .isEqualTo("profile.read");
        assertThat(schemas.path("CreateEmployeeRequestDto").path("example").path("reference").asText())
                .isEqualTo("EMP-0007");
        assertThat(schemas.path("CreatedEmployeeResponseDto").path("example").path("status").asText())
                .isEqualTo("UPCOMING");
        String createContractPath = "/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts";
        String correctContractPath = createContractPath + "/{contractId}/corrections";
        var createContract = paths.path(createContractPath).path("post");
        var correctContract = paths.path(correctContractPath).path("post");
        var expiringContracts = paths.path("/api/v1/companies/{companyId}/contracts/expiring").path("get");
        assertThat(createContract.path("summary").asText()).contains("Record contract facts");
        assertThat(createContract.path("description").asText()).contains("contract.write", "UNREVIEWED", "NOT_RECORDED");
        assertThat(correctContract.path("description").asText())
                .contains("expectedVersion", "stale", "originally created");
        assertThat(createContract.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        assertThat(correctContract.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        assertThat(schemas.path("CreateContractRequestDto").path("example").path("typeCode").asText()).isEqualTo("CDD");
        assertThat(schemas.path("CorrectContractRequestDto").path("example").path("expectedVersion").asInt()).isEqualTo(1);
        assertThat(createContract.path("responses").path("201").path("content")
                .path("application/json").path("schema").path("example").path("reviewState").asText())
                .isEqualTo("UNREVIEWED");
        assertThat(createContract.path("responses").path("404").path("description").asText()).contains("episode");
        assertThat(correctContract.path("responses").path("409").path("description").asText()).contains("Stale");
        assertThat(expiringContracts.path("summary").asText()).contains("recorded expiry");
        assertThat(expiringContracts.path("description").asText()).contains("current contract versions", "contract.read", "Africa/Casablanca");
        assertThat(expiringContracts.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        assertThat(expiringContracts.path("responses").path("200").path("content").path("application/json")
                .path("schema").path("example").path("items").get(0).path("employeeName").asText())
                .isEqualTo("Yasmine Bennani");
        for (var securedOperation : java.util.List.of(companyMemberships, preview, confirm)) {
            assertThat(securedOperation.path("security").get(0).path("keycloak").get(0).asText())
                    .isEqualTo("profile.read");
            assertThat(securedOperation.path("responses").path("401").path("description").asText())
                    .contains("access token");
        }
    }

    @Test void directorySwaggerExplainsFiltersAndPersistentDemoRecords() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        var api = mapper.readTree(response.body());
        var list = api.path("paths").path("/api/v1/companies/{companyId}/employees").path("get");
        var detail = api.path("paths").path("/api/v1/companies/{companyId}/employees/{employeeId}").path("get");
        assertThat(list.path("description").asText()).contains("employee.read", "CURRENT", "Africa/Casablanca", "duplicate names");
        assertThat(detail.path("description").asText()).contains("scoped 404", "private contact");
        assertThat(list.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        assertThat(detail.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var schemas = api.path("components").path("schemas");
        assertThat(schemas.path("EmployeeDirectoryResponseDto").path("example").path("items").get(0).path("name").asText())
                .isEqualTo("Ahmed El Amrani");
        assertThat(schemas.path("EmployeeDetailResponseDto").path("example").path("reference").asText()).isEqualTo("EMP-0002");
        assertThat(schemas.path("EmployeeSummaryResponseDto").path("properties").has("workEmail")).isFalse();
        assertThat(schemas.path("EmployeeDetailResponseDto").path("properties").has("workEmail")).isTrue();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> {
            scope.apply(jdbc, "c0000000-0000-4000-8000-000000000011",
                    java.util.UUID.fromString("c0000000-0000-4000-8000-000000000001"));
            assertThat(jdbc.queryForObject("select count(*) from employment_employee where company_id = ?::uuid", Integer.class,
                    "c0000000-0000-4000-8000-000000000001")).isEqualTo(5);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee where name = 'Ahmed El Amrani'", Integer.class)).isEqualTo(2);
        });
    }

    @Test void assistantSwaggerDocumentsOwnedConversationsAndPersistentExpiryExample() throws Exception {
        var api = mapper.readTree(get("/v3/api-docs").body());
        var paths = api.path("paths");
        var create = paths.path("/api/v1/companies/{companyId}/assistant/conversations").path("post");
        var ask = paths.path("/api/v1/companies/{companyId}/assistant/conversations/{conversationId}/contract-expiry-requests").path("post");
        assertThat(create.path("description").asText()).contains("24 hours", "contract.read", "no question");
        assertThat(ask.path("description").asText()).contains("Africa/Casablanca", "synthetic", "structured fallback");
        assertThat(ask.path("responses").path("503").path("description").asText()).contains("timed out");
        var schemas = api.path("components").path("schemas");
        assertThat(schemas.path("ContractExpiryQuestionRequestDto").path("example").path("question").asText())
                .isEqualTo("Which contracts expire in October 2026?");
        assertThat(schemas.path("ExpiryResultResponseDto").path("example").path("items").get(0).path("employeeName").asText())
                .isEqualTo("Yasmine Bennani");
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> {
            scope.apply(jdbc, "c0000000-0000-4000-8000-000000000011",
                    java.util.UUID.fromString("c0000000-0000-4000-8000-000000000001"));
            assertThat(jdbc.queryForObject("select count(*) from employment_contract_version where contract_id = ?::uuid and end_date = date '2026-10-15'",
                    Integer.class, "c0000000-0000-4000-8000-000000000090")).isEqualTo(1);
        });
    }

    @Test void swaggerUiLoadsLocallyWithoutOpeningHrRoutes() throws Exception {
        var response = get("/swagger-ui/index.html");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
        assertThat(get("/api/v1/identity").statusCode()).isEqualTo(401);
    }

    @Test void localContractDemoRemainsVisibleOnlyToTheSeededHrMembership() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        Integer contracts = transaction.execute(status -> {
            scope.apply(jdbc, "c0000000-0000-4000-8000-000000000011",
                    java.util.UUID.fromString("c0000000-0000-4000-8000-000000000001"));
            return jdbc.queryForObject("select count(*) from employment_contract where id = ?",
                    Integer.class, java.util.UUID.fromString("c0000000-0000-4000-8000-000000000073"));
        });
        assertThat(contracts).isEqualTo(1);
    }
}

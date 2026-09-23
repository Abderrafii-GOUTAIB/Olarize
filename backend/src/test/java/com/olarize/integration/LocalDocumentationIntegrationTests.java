package com.olarize.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
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
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test void localOpenApiListsOnlyTheEndpointAndBearerScheme() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("/api/v1/me", "keycloak", "authorizationCode", "profile.read")
            .doesNotContain("/api/employees", "DB_PASSWORD", "sensitive-query-value");

        var operation = mapper.readTree(response.body()).path("paths").path("/api/v1/me").path("get");
        assertThat(operation.path("summary").asText()).isEqualTo("Return the authenticated external subject");
        assertThat(operation.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var successSchema = operation.path("responses").path("200").path("content")
            .path("application/json").path("schema");
        assertThat(successSchema.isObject()).isTrue();
        assertThat(successSchema.size()).isGreaterThan(0);
        assertThat(response.body()).contains("subject");
        assertThat(operation.path("responses").path("401").path("description").asText()).isNotBlank();
        assertThat(operation.path("responses").path("403").path("description").asText()).isNotBlank();
    }

    @Test void swaggerUiLoadsLocallyWithoutOpeningHrRoutes() throws Exception {
        var response = get("/swagger-ui/index.html");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
        assertThat(get("/api/v1/me").statusCode()).isEqualTo(401);
    }
}

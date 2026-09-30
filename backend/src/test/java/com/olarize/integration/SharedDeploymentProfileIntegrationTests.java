package com.olarize.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class SharedDeploymentProfileIntegrationTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void runtimeConfiguration(DynamicPropertyRegistry properties) {
        // Shared deployments run migrations separately; this intentionally empty
        // test database checks profile behavior rather than schema validation.
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("olarize.identity.database-scope-key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.add("olarize.auth.issuer", () -> "https://identity.example.test/realms/olarize");
        properties.add("olarize.auth.jwk-set-uri", () -> "https://identity.example.test/certs");
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate database;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test void sharedProfileStartsWithoutRunningMigrations() {
        assertThat(database.queryForObject("select count(*) from pg_tables where tablename = 'flyway_schema_history'", Integer.class))
            .isZero();
    }

    @Test void healthWorksAndLocalDocumentationRemainsClosed() throws Exception {
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html", "/api/v1/identity"}) {
            var response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(response.headers().firstValue("content-type").orElse(""))
                .contains("application/problem+json");
        }
    }
}

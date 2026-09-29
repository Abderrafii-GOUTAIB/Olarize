package com.olarize.integration;

import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
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
    static final String ISSUER = "https://local-demo.example.test/realms/olarize";
    static final RSAKey SIGNING_KEY;
    static final HttpServer KEYS;
    static {
        try {
            SIGNING_KEY = new RSAKeyGenerator(2048).keyID("local-demo-test-key").generate();
            KEYS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            KEYS.createContext("/certs", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jwks.length);
                try (var output = exchange.getResponseBody()) { output.write(jwks); }
            });
            KEYS.start();
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", () -> "olarize_runtime");
        properties.add("spring.datasource.password", () -> "local-synthetic-only");
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.flyway.placeholders.local_runtime_password", () -> "local-synthetic-only");
        properties.add("olarize.auth.issuer", () -> ISSUER);
        properties.add("olarize.auth.jwk-set-uri", () -> "http://127.0.0.1:" + KEYS.getAddress().getPort() + "/certs");
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> authenticatedGet(String path, String subject) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token(subject)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String token(String subject) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject(subject).audience("olarize-api")
                .issueTime(Date.from(now.minusSeconds(2))).expirationTime(Date.from(now.plusSeconds(300)))
                .claim("typ", "Bearer").claim("azp", "olarize-web").claim("scope", "openid profile.read").build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("local-demo-test-key").build(), claims);
        jwt.sign(new RSASSASigner(SIGNING_KEY));
        return jwt.serialize();
    }

    @AfterAll static void stopKeyServer() { KEYS.stop(0); }

    @Test void localProfileSeedsPersistentRowsAndSwaggerReturnsThemThroughTheRestrictedRuntimeRole() throws Exception {
        var active = authenticatedGet("/api/v1/identity/memberships", "c0000000-0000-4000-8000-000000000011");
        assertThat(active.statusCode()).isEqualTo(200);
        assertThat(active.body()).contains("Demo Morocco SARL", "c0000000-0000-4000-8000-000000000001", "identity.admin");

        var context = authenticatedGet("/api/v1/identity/company-context", "c0000000-0000-4000-8000-000000000011");
        assertThat(context.statusCode()).isEqualTo(200);
        assertThat(context.body()).contains("Demo Morocco SARL", "identity.admin");

        var managed = authenticatedGet("/api/v1/companies/c0000000-0000-4000-8000-000000000001/memberships",
                "c0000000-0000-4000-8000-000000000011");
        assertThat(managed.statusCode()).isEqualTo(200);
        assertThat(managed.body()).contains("c0000000-0000-4000-8000-000000000011",
                "c0000000-0000-4000-8000-000000000021", "employee.self.read");
    }

    @Test void localOpenApiListsOnlyTheEndpointAndBearerScheme() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("/api/v1/identity", "keycloak", "authorizationCode", "profile.read")
            .doesNotContain("/api/employees", "DB_PASSWORD", "sensitive-query-value");

        var operation = mapper.readTree(response.body()).path("paths").path("/api/v1/identity").path("get");
        assertThat(operation.path("summary").asText()).isEqualTo("Return the authenticated external subject");
        assertThat(operation.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var successSchema = operation.path("responses").path("200").path("content")
            .path("application/json").path("schema");
        assertThat(successSchema.isObject()).isTrue();
        assertThat(successSchema.size()).isGreaterThan(0);
        assertThat(response.body()).contains("subject");
        assertThat(operation.path("responses").path("401").path("description").asText()).isNotBlank();
        assertThat(operation.path("responses").path("403").path("description").asText()).isNotBlank();

        var memberships = mapper.readTree(response.body()).path("paths").path("/api/v1/identity/memberships").path("get");
        assertThat(memberships.path("summary").asText()).contains("active company memberships");
        assertThat(memberships.path("description").asText()).contains("empty list", "validated Keycloak access token", "current database state");
        assertThat(memberships.path("security").get(0).path("keycloak").get(0).asText()).isEqualTo("profile.read");
        var membershipItem = memberships.path("responses").path("200").path("content").path("application/json")
                .path("schema").path("items");
        assertThat(membershipItem.path("$ref").asText()).isEqualTo("#/components/schemas/CompanyContext");
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
    }

    @Test void swaggerUiLoadsLocallyWithoutOpeningHrRoutes() throws Exception {
        var response = get("/swagger-ui/index.html");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
        assertThat(get("/api/v1/identity").statusCode()).isEqualTo(401);
    }
}

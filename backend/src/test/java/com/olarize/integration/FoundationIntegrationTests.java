package com.olarize.integration;

import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.flyway.enabled=true")
class FoundationIntegrationTests {
    static final RSAKey SIGNING_KEY;
    static final RSAKey UNTRUSTED_KEY;
    static final HttpServer KEYS;
    static final String ISSUER = "https://identity.example.test/realms/olarize";
    static {
        try {
            SIGNING_KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            UNTRUSTED_KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            KEYS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] body = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            KEYS.createContext("/certs", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            KEYS.start();
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    @AfterAll static void stopKeyServer() { KEYS.stop(0); }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("olarize.auth.issuer", () -> ISSUER);
        properties.add("olarize.auth.jwk-set-uri", () -> "http://127.0.0.1:" + KEYS.getAddress().getPort() + "/certs");
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.placeholders.db_scope_key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.add("olarize.identity.database-scope-key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    final HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test void healthIsPublicAndDoesNotExposeInfrastructure() throws Exception {
        var response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"")
            .doesNotContain("components", "details", "jdbc", "password");
        assertThat(response.headers().firstValue("set-cookie")).isEmpty();
    }

    @Test void readinessIncludesTheWorkingDatabase() throws Exception {
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test void privateRoutesAndOtherActuatorEndpointsAreDenied() throws Exception {
        for (String path : new String[]{"/api/employees", "/api/v1/identity/memberships", "/api/v1/identity/company-context", "/actuator/env", "/actuator", "/login", "/v3/api-docs", "/swagger-ui/index.html"}) {
            var response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
            assertThat(response.body()).contains("HTTP_401", "requestId");
            assertThat(response.headers().firstValue("X-Request-ID")).isPresent();
        }
    }

    @Test void writesAreNotPublic() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/employees"))
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json").build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("HTTP_403", "requestId");
    }

    String token(String variation) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(variation.equals("issuer") ? "https://wrong.example.test" : ISSUER)
                .subject(variation.equals("subject") ? "" : "user-123")
                .audience(variation.equals("audience") ? "another-api" : "olarize-api")
                .issueTime(Date.from(now.minusSeconds(600)))
                .claim("typ", variation.equals("id-token") ? "ID" : "Bearer")
                .claim("azp", variation.equals("client") ? "another-client" : "olarize-web")
                .claim("scope", variation.equals("scope") ? "openid" : "openid profile.read")
                .claim("email", "private@example.test");
        if (!variation.equals("expiration")) claims.expirationTime(Date.from(now.plusSeconds(variation.equals("expired") ? -300 : 300)));
        if (variation.equals("not-before")) claims.notBeforeTime(Date.from(now.plusSeconds(300)));
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims.build());
        jwt.sign(new RSASSASigner(variation.equals("signature") ? UNTRUSTED_KEY : SIGNING_KEY));
        return jwt.serialize();
    }

    HttpResponse<String> authenticatedGet(String path, String token) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).header("X-Request-ID", "auth-test")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void currentUserReturnsOnlyVerifiedUserId() throws Exception {
        var response = authenticatedGet("/api/v1/identity", token("valid"));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"currentUserId\":\"user-123\"}");
        assertThat(response.headers().firstValue("cache-control").orElse("")).contains("no-store");
        assertThat(response.headers().firstValue("set-cookie")).isEmpty();
    }

    @Test void currentUserRequiresBearerAuthentication() throws Exception {
        var response = get("/api/v1/identity");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("www-authenticate")).contains("Bearer");
        assertThat(response.body()).contains("HTTP_401", "requestId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "client", "id-token", "subject", "expiration", "expired", "not-before", "signature", "malformed"})
    void invalidTokensAreRejectedWithoutLeakingClaims(String variation) throws Exception {
        var response = authenticatedGet("/api/v1/identity", variation.equals("malformed") ? "not-a-jwt" : token(variation));
        assertThat(response.statusCode()).as(variation).isEqualTo(401);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
        assertThat(response.body()).contains("HTTP_401", "auth-test").doesNotContain("private@example.test", "user-123", "test-key");
    }

    @Test void validTokenWithoutScopeIsForbidden() throws Exception {
        var response = authenticatedGet("/api/v1/identity", token("scope"));
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("HTTP_403", "auth-test");
    }

    @Test void authenticationDoesNotGrantAccessToUnimplementedHrRoutes() throws Exception {
        assertThat(authenticatedGet("/api/employees", token("valid")).statusCode()).isEqualTo(403);
    }

    @Test void companyProvisioningHasNoHttpSurface() throws Exception {
        assertThat(get("/api/v1/companies").statusCode()).isEqualTo(401);
        assertThat(authenticatedGet("/api/v1/companies", token("valid")).statusCode()).isEqualTo(403);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/companies"))
                .header("Authorization", "Bearer " + token("valid"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Synthetic\"}")).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
    }

    @Test void clientCannotSelectAnotherSubject() throws Exception {
        assertThat(authenticatedGet("/api/v1/identity?subject=other-user", token("valid")).body())
                .isEqualTo("{\"currentUserId\":\"user-123\"}");
    }

    @Test void flywayAppliedAndVectorOperationsWork() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success and version = '1'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select extversion from pg_extension where extname = 'vector'", String.class)).isEqualTo("0.8.6");
        assertThat(jdbc.queryForObject("select '[1,2,3]'::vector <-> '[1,2,3]'::vector", Double.class)).isZero();
    }
}

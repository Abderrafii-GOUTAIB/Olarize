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
    properties = {"spring.flyway.enabled=false", "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=1000"})
class CompanyContextIntegrationTests {
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
        initialize();
        properties.add("olarize.auth.issuer", () -> ISSUER);
        properties.add("olarize.auth.jwk-set-uri", () -> "http://127.0.0.1:" + KEYS.getAddress().getPort() + "/certs");
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", () -> "membership_runtime");
        properties.add("spring.datasource.password", () -> "synthetic-only");
    }


    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.olarize.identity.application.port.in.CompanyAccessUseCase access;
    @Autowired com.olarize.identity.application.port.out.MembershipReaderPort reader;
    @Autowired com.olarize.organization.application.port.in.CompanyLookupPort companyLookup;
    final HttpClient client = HttpClient.newHttpClient();
    static JdbcTemplate owner;

    static void initialize() {
        if (owner != null) return;
        org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        owner = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        owner.execute("create role olarize_identity_provisioner nologin nosuperuser nobypassrls");
        owner.execute("create role membership_operator login password 'synthetic-only' nosuperuser nobypassrls nocreatedb nocreaterole");
        owner.execute("create role membership_runtime login password 'synthetic-only' nosuperuser nobypassrls nocreatedb nocreaterole");
        owner.execute("create role membership_migrator nologin nosuperuser nobypassrls");
        owner.execute("grant olarize_identity_provisioner to membership_operator");
        owner.execute("grant usage on schema public to membership_runtime, membership_operator, membership_migrator");
        owner.execute("grant create on schema public to membership_migrator");
        owner.execute("grant select on organization_company to membership_runtime, membership_migrator");
        owner.execute("alter table identity_membership owner to membership_migrator");
        owner.execute("alter table identity_membership_audit owner to membership_migrator");
        owner.execute("alter function public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid) owner to membership_migrator");
        owner.execute("grant select on identity_membership to membership_runtime");
        owner.execute("grant execute on function public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid) to olarize_identity_provisioner");
    }
    java.util.UUID company() {
        var id = java.util.UUID.randomUUID();
        owner.update("insert into organization_company(id,name,created_at,updated_at) values (?, 'Synthetic', now(), now())", id);
        return id;
    }
    java.util.UUID membership(String subject, java.util.UUID company, String status, String permission) {
        var id = java.util.UUID.randomUUID();
        owner.update("insert into identity_membership(id,company_id,subject,permissions,status,version,created_at,updated_at) values (?,?,?,ARRAY[?],?,1,now(),now())", id, company, subject, permission, status);
        return id;
    }
    String subject() { return "synthetic-" + java.util.UUID.randomUUID(); }
    tools.jackson.databind.JsonNode json(String value) { return new tools.jackson.databind.json.JsonMapper().readTree(value); }
    String token(String subject) throws Exception {
        String variation = "valid";
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(variation.equals("issuer") ? "https://wrong.example.test" : ISSUER)
                .subject(subject)
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


    @Test void listsOnlyOwnActiveMembershipsAndResolvesExplicitScope() throws Exception {
        String subject = subject();
        var a = company(); var b = company(); var c = company();
        membership(subject, a, "ACTIVE", "identity.admin");
        membership(subject, b, "ACTIVE", "employee.read");
        membership(subject, c, "REVOKED", "identity.admin");
        membership(subject(), c, "ACTIVE", "payroll.read");
        String token = token(subject);
        var listed = authenticatedGet("/api/v1/identity/memberships", token);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed.body()).size()).isEqualTo(2);
        assertThat(listed.body()).contains(a.toString(), b.toString()).doesNotContain(c.toString(), subject, "payroll").contains("companyName");
        var selected = authenticatedGet("/api/v1/identity/company-context?companyId=" + b, token);
        assertThat(selected.statusCode()).isEqualTo(200);
        assertThat(selected.body()).contains(b.toString(), "employee.read").doesNotContain(a.toString(), "identity.admin");
        assertThat(selected.headers().firstValue("cache-control").orElse("")).contains("no-store");
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).statusCode()).isEqualTo(400);
        assertThat(authenticatedGet("/api/v1/identity/company-context?companyId=" + c, token).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
    }
    @Test void emptySingleMalformedAndForgedSubjectRequestsAreSafe() throws Exception {
        String subject = subject();
        String token = token(subject);
        assertThat(authenticatedGet("/api/v1/identity/memberships", token).body()).isEqualTo("[]");
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).statusCode()).isEqualTo(403);
        var company = company(); membership(subject, company, "ACTIVE", "identity.admin");
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).body()).contains(company.toString());
        for (String query : new String[]{"companyId=bad", "companyId=", "subject=other", "companyId=" + company + "&subject=other", "companyId=" + company + "&companyId=" + company}) {
            var response = authenticatedGet("/api/v1/identity/company-context?" + query, token);
            assertThat(response.statusCode()).as(query).isEqualTo(400);
            assertThat(response.body()).contains("HTTP_400", "requestId").doesNotContain("other", "bad", company.toString());
        }
        assertThat(authenticatedGet("/api/v1/identity/memberships?subject=other", token).statusCode()).isEqualTo(400);
        var denied = authenticatedGet("/api/v1/identity/company-context?companyId=" + company, token(subject()));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.body()).contains("HTTP_403", "requestId").doesNotContain(company.toString());
    }
    @Test void unauthenticatedAndUnscopedTokensCannotReadMemberships() throws Exception {
        for (String path : new String[]{"/api/v1/identity/memberships", "/api/v1/identity/company-context"}) {
            assertThat(authenticatedGet(path, "invalid").statusCode()).isEqualTo(401);
            var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject(subject()).audience("olarize-api")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("typ", "Bearer")
                    .claim("azp", "olarize-web").claim("scope", "openid").build();
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
            jwt.sign(new RSASSASigner(SIGNING_KEY));
            assertThat(authenticatedGet(path, jwt.serialize()).statusCode()).isEqualTo(403);
        }
    }
    @Test void revokedMembershipAndReducedPermissionsApplyToSameTokenOnNextRequest() throws Exception {
        String subject = subject(); var company = company();
        var id = membership(subject, company, "ACTIVE", "employee.read");
        String token = token(subject);
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).body()).contains("employee.read");
        owner.update("update identity_membership set permissions=ARRAY['identity.admin'], version=2 where id=?", id);
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).body()).doesNotContain("employee.read").contains("identity.admin");
        owner.update("update identity_membership set status='REVOKED', version=3 where id=?", id);
        assertThat(authenticatedGet("/api/v1/identity/company-context?companyId=" + company, token).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet("/api/v1/identity/memberships", token).body()).isEqualTo("[]");
    }
    @Test void pooledReadsAndOrganizationLookupUseCurrentSubject() {
        String a = subject(); String b = subject(); var ca = company(); var cb = company();
        membership(a, ca, "ACTIVE", "identity.admin"); membership(b, cb, "ACTIVE", "identity.admin");
        assertThat(reader.activeFor(a)).extracting(com.olarize.identity.domain.model.Membership::companyId).containsExactly(ca);
        assertThat(reader.activeFor(b)).extracting(com.olarize.identity.domain.model.Membership::companyId).containsExactly(cb);
        assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
        var principal = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("synthetic").header("alg", "RS256").subject(a).build();
        var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(principal,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_profile.read")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            assertThat(companyLookup.find(ca)).isPresent();
            assertThat(companyLookup.find(cb)).isEmpty();
            owner.update("update identity_membership set status='REVOKED' where subject=?", a);
            assertThat(companyLookup.find(ca)).isEmpty();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> access.resolve(ca))).isInstanceOf(com.olarize.identity.domain.exception.CompanyAccessDenied.class);
        assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
    }

    @Test void concurrentCompanyLookupsDoNotHoldPoolConnectionsWhileCheckingMembership() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        var requests = new java.util.ArrayList<java.util.concurrent.Callable<HttpResponse<String>>>();
        for (int index = 0; index < 8; index++) {
            String subject = subject();
            var company = company();
            membership(subject, company, "ACTIVE", "employee.read");
            String token = token(subject);
            requests.add(() -> {
                start.await();
                return authenticatedGet("/api/v1/identity/company-context?companyId=" + company, token);
            });
        }

        var ready = new java.util.concurrent.CountDownLatch(requests.size());
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(requests.size())) {
            var futures = requests.stream().map(request -> executor.submit(() -> {
                ready.countDown();
                return request.call();
            })).toList();
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) {
                var response = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(response.statusCode()).isEqualTo(200);
            }
        }
    }

    record CommandResult(int exit, String output, String error) { }
    String commandInput(java.util.UUID company, String subject, String action, long version, java.util.UUID key) {
        return new tools.jackson.databind.json.JsonMapper().writeValueAsString(java.util.Map.of("companyId", company.toString(), "subject", subject,
            "action", action, "expectedVersion", version, "reason", "Synthetic reason", "evidence", "Synthetic Keycloak verification", "idempotencyKey", key.toString(), "confirm", true));
    }
    CommandResult command(String input, String user) {
        var output = new java.io.ByteArrayOutputStream(); var errors = new java.io.ByteArrayOutputStream();
        var env = java.util.Map.of("IDENTITY_PROVISION_DB_URL", POSTGRES.getJdbcUrl(), "IDENTITY_PROVISION_DB_USERNAME", user,
            "IDENTITY_PROVISION_DB_PASSWORD", user.equals(POSTGRES.getUsername()) ? POSTGRES.getPassword() : "synthetic-only");
        int exit = com.olarize.identity.adapter.in.cli.ProvisionMembershipCommand.run(new String[]{"provision-membership"},
            new java.io.ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), new java.io.PrintStream(output), new java.io.PrintStream(errors), env);
        return new CommandResult(exit, output.toString(StandardCharsets.UTF_8), errors.toString(StandardCharsets.UTF_8));
    }
    @Test void controlledCommandGrantsRetriesConflictsAndRevokesWithAtomicEvidence() throws Exception {
        var company = company(); var key = java.util.UUID.randomUUID(); String subject = subject();
        String input = commandInput(company, subject, "GRANT", 0, key);
        var first = command(input, "membership_operator");
        assertThat(first.exit()).as(first.error()).isZero();
        assertThat(first.error()).isEmpty();
        var result = json(first.output());
        var id = java.util.UUID.fromString(result.get("membershipId").stringValue());
        var receipt = owner.queryForMap("select * from identity_membership_audit where membership_id=?", id);
        assertThat(receipt).containsEntry("operator_name", "membership_operator").containsEntry("action", "GRANT")
                .containsEntry("correlation_id", java.util.UUID.fromString(result.get("requestId").stringValue()));
        assertThat(command(input, "membership_operator").output()).contains(id.toString());
        assertThat(command(input.replace("Synthetic reason", "Changed"), "membership_operator").exit()).isEqualTo(3);
        assertThat(command(commandInput(company, subject, "REVOKE", 0, java.util.UUID.randomUUID()), "membership_operator").exit()).isEqualTo(3);
        String token = token(subject);
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).statusCode()).isEqualTo(200);
        var revoked = command(commandInput(company, subject, "REVOKE", 1, java.util.UUID.randomUUID()), "membership_operator");
        assertThat(revoked.exit()).as(revoked.error()).isZero();
        assertThat(authenticatedGet("/api/v1/identity/company-context", token).statusCode()).isEqualTo(403);
        assertThat(json(command(input, "membership_operator").output()).get("version").longValue()).isEqualTo(1);
        assertThat(owner.queryForObject("select count(*) from identity_membership_audit where membership_id=?", Integer.class, id)).isEqualTo(2);
    }
    @Test void commandRejectsOrdinaryCredentialsOwnerAndInvalidInputsWithoutLeaks() {
        var company = company(); String subject = subject(); String input = commandInput(company, subject, "GRANT", 0, java.util.UUID.randomUUID());
        assertThat(command(input, "membership_runtime").exit()).isEqualTo(4);
        assertThat(command(input, POSTGRES.getUsername()).exit()).isEqualTo(4);
        for (String invalid : new String[]{input.replace("true", "false"), input.replace("true", "\"true\""), input.replace("\"confirm\"", "\"actor\""), input + " {}", input.replace("0", "0.5"), "{}", "x".repeat(16385)}) {
            var failure = command(invalid, "membership_operator");
            assertThat(failure.exit()).as(failure.error()).isEqualTo(2);
            assertThat(failure.output()).isEmpty();
            assertThat(failure.error()).doesNotContain(subject, "Synthetic", "sql", "Exception");
        }
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> jdbc.update("update identity_membership set status='REVOKED'"))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        var operator = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(POSTGRES.getJdbcUrl(), "membership_operator", "synthetic-only"));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> operator.execute("insert into identity_membership(id) values (gen_random_uuid())"))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(owner.queryForObject("select count(*) from identity_membership where subject=?", Integer.class, subject)).isZero();
    }
    @Test void auditFailureRollsBackAndConcurrentRetriesCreateOneMembership() throws Exception {
        var company = company(); var key = java.util.UUID.randomUUID(); String subject = subject();
        String input = commandInput(company, subject, "GRANT", 0, key);
        owner.execute("alter table identity_membership_audit add constraint synthetic_failure check (idempotency_key <> '" + key + "'::uuid)");
        try {
            assertThat(command(input, "membership_operator").exit()).isEqualTo(5);
            assertThat(owner.queryForObject("select count(*) from identity_membership where subject=?", Integer.class, subject)).isZero();
        } finally { owner.execute("alter table identity_membership_audit drop constraint synthetic_failure"); }
        var ready = new java.util.concurrent.CountDownLatch(2); var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<CommandResult> call = () -> { ready.countDown(); start.await(); return command(input, "membership_operator"); };
            var a = executor.submit(call); var b = executor.submit(call);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); start.countDown();
            var ar = a.get(10, java.util.concurrent.TimeUnit.SECONDS); var br = b.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(ar.exit()).as(ar.error()).isZero(); assertThat(br.exit()).as(br.error()).isZero();
            assertThat(json(ar.output()).get("membershipId").stringValue()).isEqualTo(json(br.output()).get("membershipId").stringValue());
        }
        assertThat(owner.queryForObject("select count(*) from identity_membership_audit where idempotency_key=?", Integer.class, key)).isEqualTo(1);
    }
}

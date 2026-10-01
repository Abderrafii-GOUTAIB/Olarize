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
import java.util.Map;
import java.util.UUID;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.flyway.enabled=false", "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=1000"})
class CompanyContextIntegrationTests {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class AssistantEvaluationConfiguration {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        com.olarize.assistant.application.port.out.ExpiryIntentPort syntheticExpiryIntent() {
            return question -> {
                if ("simulate provider failure".equals(question)) throw new com.olarize.assistant.application.exception.AssistantUnavailable();
                return new com.olarize.assistant.infrastructure.out.provider.SyntheticExpiryPlanner().plan(question);
            };
        }
    }
    static final String SCOPE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    static final RSAKey SIGNING_KEY;
    static final RSAKey UNTRUSTED_KEY;
    static final HttpServer KEYS;
    static final HttpServer KEYCLOAK_ADMIN;
    static final java.util.concurrent.atomic.AtomicBoolean IDENTITY_PROVIDER_UNAVAILABLE = new java.util.concurrent.atomic.AtomicBoolean();
    static final String ISSUER = "https://identity.example.test/realms/olarize";
    static {
        try {
            SIGNING_KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            UNTRUSTED_KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            KEYS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] jwksBody = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            KEYS.createContext("/certs", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jwksBody.length);
                try (var output = exchange.getResponseBody()) { output.write(jwksBody); }
            });
            KEYS.start();
            KEYCLOAK_ADMIN = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            KEYCLOAK_ADMIN.createContext("/token", exchange -> {
                byte[] body = (IDENTITY_PROVIDER_UNAVAILABLE.get() ? "{}" : "{\"access_token\":\"synthetic-token\"}")
                        .getBytes(StandardCharsets.UTF_8);
                int status = IDENTITY_PROVIDER_UNAVAILABLE.get() ? 503 : 200;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            KEYCLOAK_ADMIN.createContext("/admin/realms/olarize/users/", exchange -> {
                int status = exchange.getRequestURI().getPath().endsWith("/not-found") ? 404 : 200;
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, status == 404 ? -1 : body.length);
                if (status != 404) try (var output = exchange.getResponseBody()) { output.write(body); }
                else exchange.close();
            });
            KEYCLOAK_ADMIN.start();
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    @AfterAll static void stopKeyServer() { KEYS.stop(0); KEYCLOAK_ADMIN.stop(0); }

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
        properties.add("olarize.identity-provider.admin-base-url", () -> "http://127.0.0.1:" + KEYCLOAK_ADMIN.getAddress().getPort());
        properties.add("olarize.identity-provider.token-uri", () -> "http://127.0.0.1:" + KEYCLOAK_ADMIN.getAddress().getPort() + "/token");
        properties.add("olarize.identity-provider.client-id", () -> "synthetic-client");
        properties.add("olarize.identity-provider.client-secret", () -> "synthetic-secret");
        properties.add("olarize.identity.preview-secret", () -> "synthetic-preview-signing-key-32-bytes-minimum");
        properties.add("olarize.identity.database-scope-key", () -> SCOPE_KEY);
    }


    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired com.olarize.employment.application.port.out.EmployeeCorrectionPort employeeCorrections;
    @Autowired com.olarize.platform.api.DatabaseScopeSigner scope;
    @Autowired com.olarize.identity.application.port.in.CompanyAccessUseCase access;
    @Autowired com.olarize.identity.application.port.out.MembershipReaderPort reader;
    @Autowired com.olarize.organization.application.port.in.CompanyLookupPort companyLookup;
    final HttpClient client = HttpClient.newHttpClient();
    static JdbcTemplate owner;

    static void initialize() {
        if (owner != null) return;
        org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .placeholders(java.util.Map.of("db_scope_key", SCOPE_KEY)).load().migrate();
        owner = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        owner.execute("create role olarize_identity_provisioner nologin nosuperuser nobypassrls");
        owner.execute("create role membership_operator login password 'synthetic-only' nosuperuser nobypassrls nocreatedb nocreaterole");
        owner.execute("create role membership_runtime login password 'synthetic-only' nosuperuser nobypassrls nocreatedb nocreaterole");
        owner.execute("create role membership_migrator nologin nosuperuser nobypassrls");
        owner.execute("create role olarize_identity_provisioner_owner nologin nosuperuser nobypassrls");
        owner.execute("grant olarize_identity_provisioner to membership_operator");
        owner.execute("grant usage on schema public to membership_runtime, membership_operator, membership_migrator");
        owner.execute("grant usage on schema public to olarize_identity_provisioner_owner");
        owner.execute("grant create on schema public to membership_migrator");
        owner.execute("grant select on organization_company to membership_runtime, membership_migrator");
        owner.execute("alter table identity_membership owner to membership_migrator");
        owner.execute("alter table identity_membership_audit owner to membership_migrator");
        owner.execute("alter function public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid) owner to membership_migrator");
        owner.execute("grant select on identity_membership to membership_runtime");
        owner.execute("grant select, insert on assistant_conversation to membership_runtime");
        owner.execute("grant select, insert on employment_employee, employment_episode, employment_creation_audit, employment_creation_request to membership_runtime");
        owner.execute("grant update (reference, normalized_reference, name, work_email, work_phone, version, recorded_at) on employment_employee to membership_runtime");
        owner.execute("grant update (start_date, version, recorded_at) on employment_episode to membership_runtime");
        owner.execute("grant select, insert on employment_employee_version, employment_job_version to membership_runtime");
        owner.execute("grant execute on function public.olarize_can_write_employee(uuid) to membership_runtime");
        owner.execute("grant select, insert, update on employment_contract to membership_runtime");
        owner.execute("grant select, insert on employment_contract_version, employment_contract_request to membership_runtime");
        owner.execute("grant execute on function public.olarize_can_write_contract(uuid) to membership_runtime");
        owner.execute("grant execute on function public.olarize_can_read_contract(uuid) to membership_runtime");
        owner.execute("grant execute on function public.olarize_can_read_employee(uuid) to membership_runtime");
        owner.execute("grant select, insert, update on employment_account_link to membership_runtime");
        owner.execute("grant select, insert on employment_account_link_audit to membership_runtime");
        owner.execute("grant select on employment_employee to olarize_identity_provisioner_owner");
        owner.execute("grant execute on function public.olarize_can_read_employee(uuid), public.olarize_can_write_employee(uuid), public.olarize_can_read_contract(uuid) to olarize_identity_provisioner_owner");
        for (String signature : List.of("olarize_can_administer_employee_link(uuid)", "olarize_employee_link_membership(uuid,uuid,boolean)",
                "olarize_employee_link_target_allowed(uuid,uuid,boolean)", "olarize_employee_link_target_exists(uuid,uuid)")) {
            owner.execute("alter function public." + signature + " owner to olarize_identity_provisioner_owner");
            owner.execute("grant execute on function public." + signature + " to membership_runtime");
        }
        owner.execute("grant execute on function public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid) to olarize_identity_provisioner");
        owner.execute("grant select on organization_company to olarize_identity_provisioner_owner");
        owner.execute("grant select, insert, update on identity_membership to olarize_identity_provisioner_owner");
        owner.execute("grant select, insert on identity_membership_change_audit to olarize_identity_provisioner_owner");
        owner.execute("grant select, insert on identity_employee_link_authority_audit to olarize_identity_provisioner_owner");
        owner.execute("alter function public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid) owner to olarize_identity_provisioner_owner");
        owner.execute("grant execute on function public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid) to olarize_identity_provisioner");
        owner.execute("alter table identity_membership_change_audit owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.list_company_identity_memberships(text,uuid) owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.change_company_membership(text,uuid,text,text,text,bigint,bigint,text,uuid,text) owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid) owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.list_company_identity_memberships_internal(text,uuid) owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.change_company_membership_internal(text,uuid,text,text,text,bigint,bigint,text,uuid,text) owner to olarize_identity_provisioner_owner");
        owner.execute("alter function public.find_company_membership_change_retry_internal(text,uuid,text,text,text,bigint,bigint,text,uuid) owner to olarize_identity_provisioner_owner");
        owner.execute("grant execute on function public.list_company_identity_memberships(text,uuid) to membership_runtime");
        owner.execute("grant execute on function public.change_company_membership(text,uuid,text,text,text,bigint,bigint,text,uuid,text) to membership_runtime");
        owner.execute("grant execute on function public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid) to membership_runtime");
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
    UUID membership(String subject, UUID company, String status, String first, String... additional) {
        UUID id = membership(subject, company, status, first);
        var permissions = new java.util.ArrayList<String>();
        permissions.add(first);
        permissions.addAll(List.of(additional));
        owner.update("update identity_membership set permissions = ?::text[] where id = ?",
                "{" + String.join(",", permissions) + "}", id);
        return id;
    }
    String subject() { return "synthetic-" + java.util.UUID.randomUUID(); }
    tools.jackson.databind.JsonNode json(String value) { return new tools.jackson.databind.json.JsonMapper().readTree(value); }

    @Test void restrictedRuntimeQueriesRequireCurrentSubjectAndCompanyContext() {
        String currentSubject = subject();
        UUID a = company(); UUID b = company(); UUID revoked = company();
        UUID aMembership = membership(currentSubject, a, "ACTIVE", "employee.self.read");
        membership(currentSubject, b, "ACTIVE", "employee.self.read");
        membership(currentSubject, revoked, "REVOKED", "employee.self.read");
        membership(subject(), a, "ACTIVE", "identity.admin");
        var transactions = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));

        assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
        transactions.execute(status -> {
            jdbc.queryForObject("select set_config('olarize.subject', ?, true)", String.class, currentSubject);
            jdbc.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, a.toString());
            assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            scope.apply(jdbc, currentSubject, null);
            assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            scope.apply(jdbc, currentSubject, a);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForList("select company_id from identity_membership", UUID.class)).containsExactly(a);
            assertThat(jdbc.queryForObject("""
                    select count(*) from organization_company company
                    join identity_membership member on member.company_id = company.id
                    """, Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from organization_company where id = ?", Integer.class, b)).isZero();
            jdbc.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, b.toString());
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from identity_membership", Integer.class)).isZero();
            return null;
        });
        assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("update identity_membership set status = 'REVOKED' where id = ?", aMembership))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into identity_membership(id,company_id,subject,permissions,status,version,created_at,updated_at)
                values (?,?,?,ARRAY['employee.self.read'],'ACTIVE',1,now(),now())
                """, UUID.randomUUID(), a, currentSubject))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);

        owner.update("update identity_membership set status = 'REVOKED', version = version + 1 where id = ?", aMembership);
        transactions.execute(status -> {
            scope.apply(jdbc, currentSubject, a);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            return null;
        });
        assertThatThrownBy(() -> transactions.execute(status -> {
            scope.apply(jdbc, currentSubject, b);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isEqualTo(1);
            throw new IllegalStateException("Synthetic rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
    }

    @Test void auditReferencesCannotCrossCompaniesAndRuntimeCannotOwnProtectedTables() {
        UUID a = company(); UUID b = company();
        UUID member = membership(subject(), a, "ACTIVE", "employee.self.read");
        assertThatThrownBy(() -> owner.update("""
                insert into identity_membership_audit(id,membership_id,company_id,subject,operator_name,action,
                    reason,evidence,expected_version,result_version,result_status,idempotency_key,correlation_id,recorded_at)
                values (?,?,?,'synthetic','operator','GRANT','Synthetic','Synthetic',0,1,'ACTIVE',?,?,now())
                """, UUID.randomUUID(), member, b, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> owner.update("""
                insert into identity_membership_change_audit(id,membership_id,company_id,subject,actor_subject,action,
                    previous_permissions,next_permissions,next_status,reason,expected_actor_version,
                    expected_target_version,result_version,result_permissions,result_status,idempotency_key,
                    correlation_id,recorded_at)
                values (?,?,?,'synthetic','actor','GRANT',ARRAY[]::text[],ARRAY['employee.self.read'],
                    'ACTIVE','Synthetic',1,0,1,ARRAY['employee.self.read'],'ACTIVE',?,'synthetic',now())
                """, UUID.randomUUID(), member, b, UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(owner.queryForObject("""
                select count(*) from pg_class relation where relation.oid in
                    ('public.organization_company'::regclass, 'public.identity_membership'::regclass,
                     'public.identity_membership_audit'::regclass, 'public.identity_membership_change_audit'::regclass)
                  and (not relation.relrowsecurity or not relation.relforcerowsecurity
                       or pg_has_role('membership_runtime', relation.relowner, 'MEMBER'))
                """, Integer.class)).isZero();
        assertThat(owner.queryForObject("""
                select rolsuper or rolbypassrls or rolcreatedb or rolcreaterole
                from pg_roles where rolname = 'membership_runtime'
                """, Boolean.class)).isFalse();
        assertThat(owner.queryForObject("""
                select pg_has_role('membership_runtime', 'olarize_identity_provisioner', 'MEMBER')
                """, Boolean.class)).isFalse();
        assertThat(owner.queryForObject("""
                select has_table_privilege('membership_runtime', 'olarize_private.scope_signing_key', 'SELECT')
                """, Boolean.class)).isFalse();
    }

    @Test void runtimeDatabaseCredentialCannotForgeAdministratorScope() {
        UUID companyId = company();
        String administrator = subject();
        String ordinaryUser = subject();
        membership(administrator, companyId, "ACTIVE", "identity.admin");
        membership(ordinaryUser, companyId, "ACTIVE", "employee.self.read");
        var transactions = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));

        assertThatThrownBy(() -> transactions.execute(status -> {
            jdbc.queryForObject("select set_config('olarize.subject', ?, true)", String.class, administrator);
            jdbc.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, companyId.toString());
            return jdbc.queryForObject("select count(*) from public.list_company_identity_memberships(?, ?)",
                    Integer.class, administrator, companyId);
        })).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> transactions.execute(status -> {
            scope.apply(jdbc, ordinaryUser, companyId);
            return jdbc.queryForObject("select count(*) from public.list_company_identity_memberships(?, ?)",
                    Integer.class, administrator, companyId);
        })).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.queryForObject(
                "select count(*) from public.list_company_identity_memberships_internal(?, ?)",
                Integer.class, administrator, companyId))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        transactions.execute(status -> {
            scope.apply(jdbc, administrator, companyId);
            assertThat(jdbc.queryForObject("select count(*) from public.list_company_identity_memberships(?, ?)",
                    Integer.class, administrator, companyId)).isEqualTo(2);
            return null;
        });
    }

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

    HttpResponse<String> authenticatedPost(String path, String token, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).header("X-Request-ID", "membership-admin-test")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void hrCreatesMinimumEmployeeAndExactRetryReturnsOneAuditedRecord() throws Exception {
        UUID companyId = company();
        String hr = subject();
        membership(hr, companyId, "ACTIVE", "employee.write");
        String path = "/api/v1/companies/" + companyId + "/employees";
        String body = employeeRequest("EMP-0007", UUID.randomUUID(), "2026-10-05");

        var first = authenticatedPost(path, token(hr), body);
        assertThat(first.statusCode()).as(first.body()).isEqualTo(201);
        var created = json(first.body());
        assertThat(created.get("reference").asText()).isEqualTo("EMP-0007");
        assertThat(created.get("status").asText()).isEqualTo("UPCOMING");
        assertThat(created.get("version").asLong()).isEqualTo(1);
        assertThat(authenticatedPost(path, token(hr), body).body()).isEqualTo(first.body());
        assertThat(authenticatedPost(path, token(hr), body.replace("EMP-0007", "EMP-0009")).statusCode()).isEqualTo(409);
        UUID ambiguousKey = UUID.randomUUID();
        String firstText = employeeRequest("EMP-HASH", ambiguousKey, "2026-09-01")
                .replace("\"jobTitle\":\"People Operations Specialist\",\"department\":\"People Operations\"",
                        "\"jobTitle\":\"People\\nOperations\",\"department\":\"HR\"");
        String changedText = employeeRequest("EMP-HASH", ambiguousKey, "2026-09-01")
                .replace("\"jobTitle\":\"People Operations Specialist\",\"department\":\"People Operations\"",
                        "\"jobTitle\":\"People\",\"department\":\"Operations\\nHR\"");
        assertThat(authenticatedPost(path, token(hr), firstText).statusCode()).isEqualTo(201);
        assertThat(authenticatedPost(path, token(hr), changedText).statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isEqualTo(2);
        assertThat(owner.queryForObject("select count(*) from employment_episode where company_id = ?", Integer.class, companyId)).isEqualTo(2);
        assertThat(owner.queryForObject("select count(*) from employment_creation_audit where company_id = ?", Integer.class, companyId)).isEqualTo(2);
    }

    @Test void employeeAuditFailureRollsBackRecordAndUnscopedRuntimeCannotReadIt() throws Exception {
        UUID companyId = company();
        String hr = subject();
        membership(hr, companyId, "ACTIVE", "employee.write");
        String path = "/api/v1/companies/" + companyId + "/employees";
        String body = employeeRequest("EMP-ROLLBACK", UUID.randomUUID(), "2026-09-01");
        owner.execute("""
                create function reject_employee_audit() returns trigger language plpgsql as $$
                begin raise exception 'synthetic audit outage'; end; $$
                """);
        owner.execute("create trigger reject_employee_audit before insert on employment_creation_audit for each row execute function reject_employee_audit()");
        try {
            assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(500);
        } finally {
            owner.execute("drop trigger reject_employee_audit on employment_creation_audit");
            owner.execute("drop function reject_employee_audit()");
        }
        assertThat(owner.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isZero();
        assertThat(owner.queryForObject("select count(*) from employment_creation_request where company_id = ?", Integer.class, companyId)).isZero();
        assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(201);
        assertThat(jdbc.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isZero();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        transaction.execute(status -> {
            scope.apply(jdbc, hr, companyId);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isEqualTo(1);
            return null;
        });
        assertThat(jdbc.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isZero();
    }

    @Test void employeeCreationRejectsInvalidPermissionScopeAndConflictingReference() throws Exception {
        UUID companyA = company(); UUID companyB = company();
        String hrA = subject(); String hrB = subject(); String ordinary = subject();
        membership(hrA, companyA, "ACTIVE", "employee.write");
        membership(hrB, companyB, "ACTIVE", "employee.write");
        membership(ordinary, companyA, "ACTIVE", "employee.self.read");
        String pathA = "/api/v1/companies/" + companyA + "/employees";
        String pathB = "/api/v1/companies/" + companyB + "/employees";
        String first = employeeRequest("EMP-0008", UUID.randomUUID(), "2026-09-01");

        assertThat(authenticatedPost(pathA, token(ordinary), first).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(pathB, token(hrA), first).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(pathA, token(hrA), first.replace("\"confirm\":true", "\"confirm\":false")).statusCode()).isEqualTo(400);
        assertThat(authenticatedPost(pathA, token(hrA), first.replace("\"name\":\"Salma El Idrissi\"", "\"name\":\" \"")).statusCode()).isEqualTo(400);
        assertThat(authenticatedPost(pathA, token(hrA), first.replace("\"confirm\":true", "\"confirm\":true,\"companyId\":\"" + companyB + "\"")).statusCode()).isEqualTo(400);
        var invalidDate = authenticatedPost(pathA, token(hrA), first.replace("2026-09-01", "not-a-date"));
        assertThat(invalidDate.statusCode()).isEqualTo(400);
        assertThat(json(invalidDate.body()).path("errors").get(0).path("field").asText()).isEqualTo("startDate");
        assertThat(json(invalidDate.body()).path("errors").get(0).path("code").asText()).isEqualTo("Invalid");
        assertThat(invalidDate.body()).doesNotContain("not-a-date");
        var unknownField = authenticatedPost(pathA, token(hrA), first.replace("\"confirm\":true", "\"confirm\":true,\"privateNote\":\"private-data\""));
        assertThat(unknownField.statusCode()).isEqualTo(400);
        assertThat(json(unknownField.body()).path("errors").get(0).path("field").asText()).isEqualTo("privateNote");
        assertThat(unknownField.body()).doesNotContain("private-data");
        assertThat(authenticatedPost(pathA, token(hrA), first).statusCode()).isEqualTo(201);
        assertThat(authenticatedPost(pathA, token(hrA), employeeRequest("emp-0008", UUID.randomUUID(), "2026-09-01")).statusCode()).isEqualTo(409);
        assertThat(authenticatedPost(pathB, token(hrB), employeeRequest("EMP-0008", UUID.randomUUID(), "2026-09-01")).statusCode()).isEqualTo(201);
        assertThat(owner.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyA)).isEqualTo(1);
    }

    @Test void concurrentCreatesSerializeSameRetriesAndCompanyReferences() throws Exception {
        UUID companyId = company();
        String hr = subject();
        membership(hr, companyId, "ACTIVE", "employee.write");
        String path = "/api/v1/companies/" + companyId + "/employees";
        String bearer = token(hr);

        String retry = employeeRequest("EMP-SAME-KEY", UUID.randomUUID(), "2026-09-01");
        // Use the same key and exact payload for both simultaneous submissions.
        var retryBody = json(retry);
        String retryPayload = employeeRequest("EMP-SAME-KEY",
                UUID.fromString(retryBody.path("idempotencyKey").asText()), "2026-09-01");
        var retryResponses = concurrentPosts(path, bearer, retryPayload, retryPayload);
        assertThat(retryResponses).extracting(HttpResponse::statusCode).containsExactly(201, 201);
        assertThat(retryResponses.get(0).body()).isEqualTo(retryResponses.get(1).body());
        assertThat(owner.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isEqualTo(1);
        assertThat(owner.queryForObject("select count(*) from employment_creation_audit where company_id = ?", Integer.class, companyId)).isEqualTo(1);

        String duplicateA = employeeRequest("EMP-RACE", UUID.randomUUID(), "2026-09-01");
        String duplicateB = employeeRequest("emp-race", UUID.randomUUID(), "2026-09-01");
        var duplicateResponses = concurrentPosts(path, bearer, duplicateA, duplicateB);
        assertThat(duplicateResponses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
        assertThat(owner.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyId)).isEqualTo(2);
        assertThat(owner.queryForObject("select count(*) from employment_episode where company_id = ?", Integer.class, companyId)).isEqualTo(2);
        assertThat(owner.queryForObject("select count(*) from employment_creation_audit where company_id = ?", Integer.class, companyId)).isEqualTo(2);
    }

    private List<HttpResponse<String>> concurrentPosts(String path, String bearer, String first, String second)
            throws Exception {
        var headers = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + bearer).header("Content-Type", "application/json");
        var firstRequest = headers.copy().POST(HttpRequest.BodyPublishers.ofString(first)).build();
        var secondRequest = headers.copy().POST(HttpRequest.BodyPublishers.ofString(second)).build();
        var firstResponse = client.sendAsync(firstRequest, HttpResponse.BodyHandlers.ofString());
        var secondResponse = client.sendAsync(secondRequest, HttpResponse.BodyHandlers.ofString());
        return List.of(firstResponse.get(), secondResponse.get());
    }

    private String employeeRequest(String reference, UUID key, String startDate) {
        return """
            {"reference":"%s","name":"Salma El Idrissi","startDate":"%s",
             "jobTitle":"People Operations Specialist","department":"People Operations",
             "site":"Casablanca","workEmail":"salma@demo.olarize.invalid",
             "idempotencyKey":"%s","confirm":true}
            """.formatted(reference, startDate, key);
    }

    private record EmployeeEpisode(UUID employeeId, UUID episodeId) { }

    private EmployeeEpisode employeeEpisode(UUID companyId, String reference) {
        UUID employeeId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        owner.update("""
                insert into employment_employee(id, company_id, reference, normalized_reference,
                    name, version, recorded_at) values (?, ?, ?, ?, 'Synthetic Employee', 1, now())
                """, employeeId, companyId, reference, reference.toLowerCase(java.util.Locale.ROOT));
        owner.update("""
                insert into employment_episode(id, employee_id, company_id, start_date, version, recorded_at)
                values (?, ?, ?, DATE '2026-01-01', 1, now())
                """, episodeId, employeeId, companyId);
        return new EmployeeEpisode(employeeId, episodeId);
    }

    private String correctionPath(UUID companyId, EmployeeEpisode employee) {
        return "/api/v1/companies/" + companyId + "/employees/" + employee.employeeId()
                + "/episodes/" + employee.episodeId() + "/corrections";
    }

    private String correctionRequest(UUID key, long version, String reference, String name,
            String start, String effective, String job) {
        return """
                {"reference":"%s","name":"%s","startDate":"%s","effectiveDate":"%s",
                 "jobTitle":%s,"department":"Operations","workEmail":"work@demo.invalid",
                 "expectedVersion":%d,"expectedEpisodeVersion":1,"reason":"Reviewed synthetic source",
                 "idempotencyKey":"%s","confirm":true}
                """.formatted(reference, name, start, effective, job == null ? "null" : "\"" + job + "\"", version, key);
    }

    @Test void employeeCorrectionsPreserveFactsAndSelectDatedJobs() throws Exception {
        UUID companyId = company(); String hr = subject();
        membership(hr, companyId, "ACTIVE", "employee.write", "employee.read", "contract.write");
        var employee = employeeEpisode(companyId, "EMP-CORRECT");
        String path = correctionPath(companyId, employee); String bearer = token(hr);
        String first = correctionRequest(UUID.randomUUID(), 1, "0007", "سلمى", "2026-01-01", "2026-02-01", "Analyst");
        var saved = authenticatedPost(path, bearer, first);
        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
        assertThat(json(saved.body()).path("version").asLong()).isEqualTo(2);
        assertThat(saved.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(authenticatedPost(path, bearer, first).body()).isEqualTo(saved.body());
        String second = correctionRequest(UUID.randomUUID(), 2, "0007", "سلمى", "2026-01-01", "2026-06-01", "Lead");
        assertThat(authenticatedPost(path, bearer, second).statusCode()).isEqualTo(200);
        String detail = "/api/v1/companies/" + companyId + "/employees/" + employee.employeeId();
        assertThat(authenticatedGet(detail + "?asOf=2026-03-01", bearer).body()).contains("Analyst").doesNotContain("Lead");
        assertThat(authenticatedGet(detail + "?asOf=2026-07-01", bearer).body()).contains("Lead");
        assertThat(authenticatedGet(detail + "?asOf=2026-01-15", bearer).body()).doesNotContain("Analyst", "Lead");
        // A later retroactive correction appends evidence; earlier source versions remain byte-for-byte unchanged.
        var frozen = owner.queryForMap("select * from employment_employee_version where employee_id = ? and version = 2", employee.employeeId());
        String retro = correctionRequest(UUID.randomUUID(), 3, "0007", "سلمى", "2026-01-01", "2026-02-01", "Senior Analyst");
        assertThat(authenticatedPost(path, bearer, retro).statusCode()).isEqualTo(200);
        assertThat(owner.queryForMap("select * from employment_employee_version where employee_id = ? and version = 2", employee.employeeId())).isEqualTo(frozen);
        assertThat(authenticatedGet(detail + "?asOf=2026-03-01", bearer).body()).contains("Senior Analyst");
        assertThat(authenticatedGet(detail + "?asOf=2026-07-01", bearer).body()).contains("Lead");
        assertThat(authenticatedPost(path, bearer, first).body()).isEqualTo(saved.body());
        assertThat(authenticatedPost(path, bearer, first.replace("سلمى", "Other")).statusCode()).isEqualTo(409);
        assertThat(authenticatedPost(path, bearer, correctionRequest(UUID.randomUUID(), 1, "0007", "Stale", "2026-01-01", "2026-02-01", "Analyst")).statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select name from employment_employee where id = ?", String.class, employee.employeeId())).isEqualTo("سلمى");
        assertThat(owner.queryForObject("select count(*) from employment_employee_version where employee_id = ?", Integer.class, employee.employeeId())).isEqualTo(4);
        var audit = owner.queryForMap("select actor_subject, reason, correlation_id from employment_employee_version where employee_id = ? and version = 4", employee.employeeId());
        assertThat(audit.get("actor_subject")).isEqualTo(hr);
        assertThat(audit.get("reason")).isEqualTo("Reviewed synthetic source");
        assertThat(audit.get("correlation_id")).isNotNull();
    }

    @Test void employeeCorrectionValidationPermissionsAndCrossCompanyIsolation() throws Exception {
        UUID a = company(); UUID b = company(); String hr = subject(); String manager = subject();
        membership(hr, a, "ACTIVE", "employee.write", "employee.read");
        membership(hr, b, "ACTIVE", "employee.write");
        membership(manager, a, "ACTIVE", "employee.direct-reports.read");
        var employee = employeeEpisode(a, "EMP-A"); var duplicate = employeeEpisode(a, "EMP-DUP");
        var foreign = employeeEpisode(b, "EMP-B");
        String path = correctionPath(a, employee); String body = correctionRequest(UUID.randomUUID(), 1, "EMP-A", "Correct", "2026-01-01", "2026-02-01", "Analyst");
        var unauth = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(unauth.statusCode()).isEqualTo(401);
        assertThat(authenticatedPost(path, token(manager), body).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(correctionPath(a, foreign), token(hr), body).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(path.replace(employee.episodeId().toString(), foreign.episodeId().toString()), token(hr), body).statusCode()).isEqualTo(404);
        for (String invalid : List.of(body.replace("\"confirm\":true", "\"confirm\":false"),
                body.replace("Correct", " "), body.replace("2026-01-01", "2026-02-30"),
                body.replace("work@demo.invalid", "not-email"), body.replace("\"reason\":", "\"salary\":100,\"reason\":"),
                body.replace("\"reason\":", "\"departmentId\":\"" + b + "\",\"reason\":"),
                body.replace("Correct", "x".repeat(201)), body.replace("\"reason\":", "\"workPhone\":\"abc123\",\"reason\":"))) {
            var result = authenticatedPost(path, token(hr), invalid);
            assertThat(result.statusCode()).as(result.body()).isEqualTo(400);
            assertThat(result.body()).doesNotContain("not-email", "abc123");
        }
        assertThat(authenticatedPost(path, token(hr), body.replace("EMP-A", " emp-dup ")).statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select version from employment_employee where id = ?", Long.class, employee.employeeId())).isEqualTo(1);
        assertThat(owner.queryForObject("select count(*) from employment_employee_version where employee_id = ?", Integer.class, employee.employeeId())).isZero();
        assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(200);
        owner.update("update identity_membership set status = 'REVOKED' where subject = ?", hr);
        assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(403);
    }

    @Test void employeeStartCorrectionReportsConflictsAndPreservesContracts() throws Exception {
        UUID c = company(); String hr = subject(); membership(hr, c, "ACTIVE", "employee.write", "employee.read", "contract.write");
        var e = employeeEpisode(c, "EMP-START"); String path = correctionPath(c, e); String bearer = token(hr);
        var contract = authenticatedPost(contractPath(c, e), bearer, createContractRequest(UUID.randomUUID(), "2026-12-31"));
        assertThat(contract.statusCode()).isEqualTo(201);
        String first = correctionRequest(UUID.randomUUID(), 1, "EMP-START", "Correct", "2025-12-01", "2026-02-01", "Analyst");
        var saved = authenticatedPost(path, bearer, first);
        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
        assertThat(json(saved.body()).path("episodeVersion").asLong()).isEqualTo(2);
        var conflict = authenticatedPost(path, bearer, correctionRequest(UUID.randomUUID(), 2, "EMP-START", "Correct", "2026-03-01", "2026-04-01", "Lead")
                .replace("\"expectedEpisodeVersion\":1", "\"expectedEpisodeVersion\":2"));
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.body()).contains("JOB_INTERVAL", "requestId");
        assertThat(owner.queryForObject("select start_date::text from employment_contract_version where employee_id = ?", String.class, e.employeeId())).isEqualTo("2026-01-01");
        owner.update("update employment_episode set last_employed_day = date '2026-05-01' where id = ?", e.episodeId());
        var lastDay = authenticatedPost(path, bearer, correctionRequest(UUID.randomUUID(), 2, "EMP-START", "Correct", "2026-06-01", "2026-06-01", "Lead")
                .replace("\"expectedEpisodeVersion\":1", "\"expectedEpisodeVersion\":2"));
        assertThat(lastDay.body()).contains("LAST_EMPLOYED_DAY");
        UUID other = UUID.randomUUID();
        owner.update("insert into employment_episode(id, employee_id, company_id, start_date, last_employed_day, version, recorded_at) values(?,?,?,date '2025-01-01',date '2025-11-30',1,now())", other, e.employeeId(), c);
        var overlap = authenticatedPost(path, bearer, correctionRequest(UUID.randomUUID(), 2, "EMP-START", "Correct", "2025-11-01", "2026-02-01", "Analyst")
                .replace("\"expectedEpisodeVersion\":1", "\"expectedEpisodeVersion\":2"));
        assertThat(overlap.body()).contains("OTHER_EPISODE");
    }

    @Test void employeeCorrectionFirstResultMatchesPersistedRetryAtMicrosecondPrecision() {
        UUID c = company(); String hr = subject(); membership(hr, c, "ACTIVE", "employee.write");
        var e = employeeEpisode(c, "EMP-PRECISION");
        UUID key = UUID.randomUUID();
        var input = new com.olarize.employment.domain.model.EmployeeCorrection(
                com.olarize.employment.domain.model.EmployeeCreation.of("EMP-PRECISION", "Synthetic Employee",
                        java.time.LocalDate.parse("2026-01-01"), null, null, null, null, null),
                java.time.LocalDate.parse("2026-02-01"), "Reviewed synthetic timestamp regression");
        Instant nanoseconds = Instant.parse("2026-10-01T15:49:08.123456789Z");
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        var first = tx.execute(status -> {
            scope.apply(jdbc, hr, c);
            return employeeCorrections.correct(c, e.employeeId(), e.episodeId(), 1, 1,
                    input, hr, key, "a".repeat(64), "precision-test", nanoseconds);
        });
        var retry = tx.execute(status -> {
            scope.apply(jdbc, hr, c);
            return employeeCorrections.findRetry(c, hr, key).orElseThrow().result();
        });
        assertThat(first.recordedAt()).isEqualTo(Instant.parse("2026-10-01T15:49:08.123456Z"));
        assertThat(retry).isEqualTo(first);
        assertThat(owner.queryForObject("select recorded_at from employment_employee where id = ?", java.sql.Timestamp.class,
                e.employeeId()).toInstant()).isEqualTo(first.recordedAt());
    }

    @Test void employeeCorrectionAuditFailureRollsBackAndConcurrencyIsSafe() throws Exception {
        UUID c = company(); String hr = subject(); membership(hr, c, "ACTIVE", "employee.write");
        var e = employeeEpisode(c, "EMP-RACE"); String path = correctionPath(c, e); String bearer = token(hr);
        String first = correctionRequest(UUID.randomUUID(), 1, "EMP-RACE", "First", "2026-01-01", "2026-02-01", "Analyst");
        owner.execute("create function reject_employee_correction() returns trigger language plpgsql as $$ begin if NEW.version > 1 then raise exception 'synthetic audit outage'; end if; return NEW; end $$");
        owner.execute("create trigger reject_employee_correction before insert on employment_employee_version for each row execute function reject_employee_correction()");
        try {
            assertThat(authenticatedPost(path, bearer, first).statusCode()).isEqualTo(500);
            assertThat(owner.queryForObject("select version from employment_employee where id = ?", Long.class, e.employeeId())).isEqualTo(1);
            assertThat(owner.queryForObject("select count(*) from employment_employee_version where employee_id = ?", Integer.class, e.employeeId())).isZero();
            assertThat(owner.queryForObject("select count(*) from employment_job_version where episode_id = ?", Integer.class, e.episodeId())).isZero();
        } finally {
            owner.execute("drop trigger reject_employee_correction on employment_employee_version");
            owner.execute("drop function reject_employee_correction()");
        }
        var same = concurrentPosts(path, bearer, first, first);
        assertThat(same).extracting(HttpResponse::statusCode).containsExactly(200, 200);
        assertThat(same.get(0).body()).isEqualTo(same.get(1).body());
        String nextA = correctionRequest(UUID.randomUUID(), 2, "EMP-RACE", "Next A", "2026-01-01", "2026-03-01", "Lead");
        String nextB = correctionRequest(UUID.randomUUID(), 2, "EMP-RACE", "Next B", "2026-01-01", "2026-03-01", "Lead");
        assertThat(concurrentPosts(path, bearer, nextA, nextB)).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409);
        assertThat(owner.queryForObject("select version from employment_employee where id = ?", Long.class, e.employeeId())).isEqualTo(3);
    }

    @Test void employeeCreationRetryKeepsOriginalFactsAfterCorrection() throws Exception {
        UUID c = company(); String hr = subject(); membership(hr, c, "ACTIVE", "employee.write", "employee.read");
        String bearer = token(hr); String createPath = "/api/v1/companies/" + c + "/employees";
        String body = employeeRequest("EMP-ORIGINAL", UUID.randomUUID(), "2026-01-01");
        var original = authenticatedPost(createPath, bearer, body);
        assertThat(original.statusCode()).isEqualTo(201);
        var e = new EmployeeEpisode(UUID.fromString(json(original.body()).path("employeeId").asText()),
                UUID.fromString(json(original.body()).path("episodeId").asText()));
        String correction = correctionRequest(UUID.randomUUID(), 1, "EMP-CHANGED", "Corrected Name", "2025-12-01", "2026-02-01", "Lead");
        assertThat(authenticatedPost(correctionPath(c, e), bearer, correction).statusCode()).isEqualTo(200);
        assertThat(authenticatedPost(createPath, bearer, body).body()).isEqualTo(original.body());
        var earlyDetail = authenticatedGet(createPath + "/" + e.employeeId() + "?asOf=2025-12-15", bearer);
        assertThat(earlyDetail.statusCode()).isEqualTo(200);
        assertThat(earlyDetail.body()).doesNotContain("People Operations Specialist", "Operations", "Lead");
        String nameOnly = correctionRequest(UUID.randomUUID(), 2, "EMP-CHANGED", "New Name", "2025-12-01", "2026-02-01", "Lead")
                .replace("\"expectedEpisodeVersion\":1", "\"expectedEpisodeVersion\":2");
        assertThat(authenticatedPost(correctionPath(c, e), bearer, nameOnly).statusCode()).isEqualTo(200);
        assertThat(owner.queryForObject("select count(*) from employment_job_version where episode_id = ?", Integer.class, e.episodeId())).isEqualTo(2);
        assertThat(authenticatedPost(createPath, bearer, body).body()).isEqualTo(original.body());
    }

    @Test void employeeCorrectionHistoryAndJobsEnforceRestrictedDatabaseScope() throws Exception {
        UUID a = company(); UUID b = company(); String hr = subject(); String readerSubject = subject();
        membership(hr, a, "ACTIVE", "employee.write"); membership(hr, b, "ACTIVE", "employee.write");
        membership(readerSubject, a, "ACTIVE", "employee.read");
        var e = employeeEpisode(a, "EMP-HISTORY");
        assertThat(authenticatedPost(correctionPath(a, e), token(hr), correctionRequest(UUID.randomUUID(), 1, "EMP-HISTORY", "History", "2026-01-01", "2026-02-01", "Analyst")).statusCode()).isEqualTo(200);
        var tx = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        tx.executeWithoutResult(status -> {
            assertThat(jdbc.queryForObject("select count(*) from employment_employee_version", Integer.class)).isZero();
            scope.apply(jdbc, hr, a);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee_version where employee_id = ?", Integer.class, e.employeeId())).isEqualTo(2);
            scope.apply(jdbc, hr, b);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee_version where employee_id = ?", Integer.class, e.employeeId())).isZero();
            scope.apply(jdbc, readerSubject, a);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee_version", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from employment_job_version where episode_id = ?", Integer.class, e.episodeId())).isEqualTo(1);
            jdbc.queryForObject("select set_config('olarize.subject', ?, true)", String.class, hr);
            assertThat(jdbc.queryForObject("select count(*) from employment_job_version", Integer.class)).isZero();
        });
        assertThat(jdbc.queryForObject("select count(*) from employment_job_version", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("update employment_employee_version set reason = 'Changed' where employee_id = ?", e.employeeId()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private String linkPath(UUID company, UUID membership) {
        return "/api/v1/companies/" + company + "/employee-account-links/" + membership;
    }
    private String linkRequest(String action, UUID employee, long version, long membershipVersion, UUID key) {
        return """
                {"action":"%s","employeeId":%s,"expectedVersion":%d,"expectedMembershipVersion":%d,
                 "reason":"Reviewed synthetic identity","verificationEvidenceReference":"SYNTHETIC-VERIFICATION-001",
                 "idempotencyKey":"%s","confirm":true}
                """.formatted(action, employee == null ? "null" : "\"" + employee + "\"", version, membershipVersion, key);
    }

    @Test void operatorExplicitlyProvisionsLinkAuthorityWithImmutableRetryAndCurrentTokenPermissions() throws Exception {
        var operator = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), "membership_operator", "synthetic-only"));
        UUID c = company(); UUID foreign = company(); String admin = subject();
        UUID member = membership(admin, c, "ACTIVE", "identity.admin");
        UUID target = membership(subject(), c, "ACTIVE", "employee.self.read");
        String bearer = token(admin); String path = linkPath(c, target);
        assertThat(authenticatedGet(path, bearer).statusCode()).isEqualTo(403);
        String sql = "select * from public.provision_employee_link_authority(?,?,?,?::bigint,?,?,?,?)";
        UUID key = UUID.randomUUID(); UUID correlation = UUID.randomUUID();
        Object[] grant = {c, member, "GRANT", 1L, "Explicit reviewed operator", "Synthetic verified subject evidence", key, correlation};
        var saved = operator.queryForMap(sql, grant);
        assertThat(saved.get("version")).isEqualTo(2L);
        assertThat(authenticatedGet(path, bearer).statusCode()).isEqualTo(200);
        assertThat(operator.queryForMap(sql, grant).get("version")).isEqualTo(2L);
        assertThatThrownBy(() -> operator.queryForMap(sql, c, member, "GRANT", 1L, "Changed", "Synthetic verified subject evidence", key, correlation))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> operator.queryForMap(sql, foreign, member, "GRANT", 2L, "Reviewed", "Evidence", UUID.randomUUID(), correlation))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.queryForMap(sql, grant)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> operator.update("update identity_membership set permissions = array['identity.link.admin'] where id = ?", member))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        operator.queryForMap(sql, c, member, "REVOKE", 2L, "Reviewed revocation", "Evidence", UUID.randomUUID(), correlation);
        assertThat(authenticatedGet(path, bearer).statusCode()).isEqualTo(403);
        assertThat(operator.queryForMap(sql, grant).get("version")).isEqualTo(2L);
        assertThat((String[]) owner.queryForObject("select permissions from identity_membership where id = ?", java.sql.Array.class, member).getArray())
                .containsExactly("identity.admin");
        assertThat(owner.queryForObject("select count(*) from identity_employee_link_authority_audit where membership_id = ?", Integer.class, member)).isEqualTo(2);
        assertThatThrownBy(() -> operator.update("delete from identity_employee_link_authority_audit where membership_id = ?", member))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test void linkAuthorityAuditFailureRollsBackAndStaleVersionsRejectConcurrentGrant() throws Exception {
        var operator = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), "membership_operator", "synthetic-only"));
        UUID c = company(); UUID member = membership(subject(), c, "ACTIVE", "employee.self.read");
        String sql = "select * from public.provision_employee_link_authority(?,?,?,?::bigint,?,?,?,?)";
        owner.execute("create function reject_link_authority_audit() returns trigger language plpgsql as $$ begin raise exception 'synthetic authority audit outage'; end $$");
        owner.execute("create trigger reject_link_authority_audit before insert on identity_employee_link_authority_audit for each row execute function reject_link_authority_audit()");
        try {
            assertThatThrownBy(() -> operator.queryForMap(sql, c, member, "GRANT", 1L, "Reviewed", "Evidence", UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(owner.queryForObject("select version from identity_membership where id = ?", Long.class, member)).isEqualTo(1L);
        } finally {
            owner.execute("drop trigger reject_link_authority_audit on identity_employee_link_authority_audit");
            owner.execute("drop function reject_link_authority_audit()");
        }
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                try { operator.queryForMap(sql, c, member, "GRANT", 1L, "Reviewed", "Evidence", UUID.randomUUID(), UUID.randomUUID()); return true; }
                catch (org.springframework.dao.DataAccessException stale) { return false; }
            };
            var first = executor.submit(attempt); var second = executor.submit(attempt);
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(owner.queryForObject("select count(*) from identity_employee_link_authority_audit where membership_id = ?", Integer.class, member)).isEqualTo(1);
    }

    @Test void administratorLinksRelinksRevokesAndOwnResolutionUsesCurrentState() throws Exception {
        UUID c = company(); String admin = subject(); String employee = subject();
        membership(admin, c, "ACTIVE", "identity.link.admin");
        UUID member = membership(employee, c, "ACTIVE", "employee.self.read");
        var first = employeeEpisode(c, "EMP-LINK-1"); var second = employeeEpisode(c, "EMP-LINK-2");
        String path = linkPath(c, member); String own = "/api/v1/companies/" + c + "/my-employee-link";
        String oldSession = token(employee); String adminToken = token(admin);
        assertThat(authenticatedGet(own, oldSession).statusCode()).isEqualTo(404);
        assertThat(authenticatedGet(path, adminToken).body()).contains("UNLINKED");
        String body = linkRequest("LINK", first.employeeId(), 0, 1, UUID.randomUUID());
        var saved = authenticatedPost(path + "/changes", adminToken, body);
        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
        assertThat(json(saved.body()).path("version").asLong()).isEqualTo(1);
        assertThat(saved.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(authenticatedPost(path + "/changes", adminToken, body).body()).isEqualTo(saved.body());
        var ownResponse = authenticatedGet(own, oldSession);
        assertThat(ownResponse.statusCode()).isEqualTo(200);
        assertThat(json(ownResponse.body()).size()).isEqualTo(2);
        assertThat(ownResponse.body()).contains(first.employeeId().toString()).doesNotContain("Synthetic Employee", "reason", "SYNTHETIC-VERIFICATION");
        String relink = linkRequest("RELINK", second.employeeId(), 1, 1, UUID.randomUUID());
        assertThat(authenticatedPost(path + "/changes", adminToken, relink).statusCode()).isEqualTo(200);
        assertThat(authenticatedGet(own, oldSession).body()).contains(second.employeeId().toString()).doesNotContain(first.employeeId().toString());
        var audit = owner.queryForMap("select previous_employee_id, employee_id, actor_subject, membership_version, verification_evidence_reference from employment_account_link_audit where membership_id = ? and version = 2", member);
        assertThat(audit.get("previous_employee_id")).isEqualTo(first.employeeId());
        assertThat(audit.get("employee_id")).isEqualTo(second.employeeId());
        assertThat(audit.get("actor_subject")).isEqualTo(admin);
        assertThat(authenticatedPost(path + "/changes", adminToken, body).body()).isEqualTo(saved.body());
        assertThat(authenticatedPost(path + "/changes", adminToken, body.replace(first.employeeId().toString(), second.employeeId().toString())).statusCode()).isEqualTo(409);
        assertThat(authenticatedPost(path + "/changes", adminToken, linkRequest("REVOKE", null, 2, 1, UUID.randomUUID())).statusCode()).isEqualTo(200);
        assertThat(authenticatedGet(own, oldSession).statusCode()).isEqualTo(404);
        assertThat(authenticatedGet(path, adminToken).body()).contains("REVOKED", second.employeeId().toString());
        assertThat(owner.queryForObject("select count(*) from employment_account_link_audit where membership_id = ?", Integer.class, member)).isEqualTo(3);
        assertThat((String[]) owner.queryForObject("select permissions from identity_membership where id = ?", java.sql.Array.class, member).getArray()).containsExactly("employee.self.read");
        assertThat(authenticatedGet("/api/v1/companies/" + c + "/employees/" + first.employeeId(), adminToken).statusCode()).isEqualTo(403);
    }

    @Test void linkingRejectsUnauthorizedSelfForeignMalformedAndStaleTargets() throws Exception {
        UUID a = company(); UUID b = company(); String admin = subject(); String hr = subject(); String employee = subject();
        UUID adminMember = membership(admin, a, "ACTIVE", "identity.link.admin");
        membership(hr, a, "ACTIVE", "employee.write");
        UUID member = membership(employee, a, "ACTIVE", "employee.self.read");
        UUID foreignMember = membership(subject(), b, "ACTIVE", "employee.self.read");
        UUID revoked = membership(subject(), a, "REVOKED", "employee.self.read");
        var e = employeeEpisode(a, "EMP-LINK-A"); var foreignEmployee = employeeEpisode(b, "EMP-LINK-B");
        String path = linkPath(a, member) + "/changes"; String body = linkRequest("LINK", e.employeeId(), 0, 1, UUID.randomUUID());
        assertThat(authenticatedPost(path, "invalid-token", body).statusCode()).isEqualTo(401);
        assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(path, token(employee), body).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(linkPath(a, adminMember) + "/changes", token(admin), body).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(linkPath(a, foreignMember) + "/changes", token(admin), body).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(path, token(admin), body.replace(e.employeeId().toString(), foreignEmployee.employeeId().toString())).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(linkPath(a, UUID.randomUUID()) + "/changes", token(admin), body).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(linkPath(a, revoked) + "/changes", token(admin), body).statusCode()).isEqualTo(409);
        for (String invalid : List.of(body.replace("\"confirm\":true", "\"confirm\":false"), body.replace("LINK", "UNKNOWN"),
                body.replace("\"reason\":", "\"subject\":\"spoofed\",\"reason\":"),
                body.replace(e.employeeId().toString(), "not-a-uuid"), body.replace("Reviewed synthetic identity", " "))) {
            assertThat(authenticatedPost(path, token(admin), invalid).statusCode()).isEqualTo(400);
        }
        assertThat(authenticatedPost(path, token(admin), linkRequest("LINK", e.employeeId(), 0, 2, UUID.randomUUID())).statusCode()).isEqualTo(409);
        assertThat(authenticatedPost(path, token(admin), body).statusCode()).isEqualTo(200);
        assertThat(authenticatedPost(path, token(admin), linkRequest("REVOKE", null, 0, 1, UUID.randomUUID())).statusCode()).isEqualTo(409);
        assertThat(authenticatedGet("/api/v1/companies/" + a + "/my-employee-link?subject=spoofed", token(hr)).statusCode()).isEqualTo(404);
        owner.update("update identity_membership set status = 'REVOKED', version = version + 1 where id = ?", member);
        assertThat(authenticatedGet("/api/v1/companies/" + a + "/my-employee-link", token(employee)).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(path, token(admin), linkRequest("REVOKE", null, 1, 2, UUID.randomUUID())).statusCode()).isEqualTo(200);
        owner.update("update identity_membership set status = 'REVOKED' where id = ?", adminMember);
        assertThat(authenticatedPost(path, token(admin), body).statusCode()).isEqualTo(403);
    }

    @Test void linkingRequiresProviderVerificationAndExactRetrySurvivesProviderOutage() throws Exception {
        UUID c = company(); String admin = subject(); membership(admin, c, "ACTIVE", "identity.link.admin");
        UUID member = membership(subject(), c, "ACTIVE", "employee.self.read");
        UUID missingAccount = membership("not-found", c, "ACTIVE", "employee.self.read");
        var e = employeeEpisode(c, "EMP-PROVIDER-LINK"); String path = linkPath(c, member) + "/changes";
        String body = linkRequest("LINK", e.employeeId(), 0, 1, UUID.randomUUID());
        assertThat(authenticatedPost(linkPath(c, missingAccount) + "/changes", token(admin), body).statusCode()).isEqualTo(404);
        IDENTITY_PROVIDER_UNAVAILABLE.set(true);
        try {
            assertThat(authenticatedPost(path, token(admin), body).statusCode()).isEqualTo(503);
            assertThat(owner.queryForObject("select count(*) from employment_account_link where company_id = ?", Integer.class, c)).isZero();
        } finally { IDENTITY_PROVIDER_UNAVAILABLE.set(false); }
        var saved = authenticatedPost(path, token(admin), body);
        assertThat(saved.statusCode()).isEqualTo(200);
        IDENTITY_PROVIDER_UNAVAILABLE.set(true);
        try { assertThat(authenticatedPost(path, token(admin), body).body()).isEqualTo(saved.body()); }
        finally { IDENTITY_PROVIDER_UNAVAILABLE.set(false); }
    }

    @Test void linkAuditRollbackAndConcurrentUniquenessAreAtomic() throws Exception {
        UUID c = company(); String admin = subject(); membership(admin, c, "ACTIVE", "identity.link.admin");
        UUID member = membership(subject(), c, "ACTIVE", "employee.self.read");
        UUID secondMember = membership(subject(), c, "ACTIVE", "employee.self.read");
        var e = employeeEpisode(c, "EMP-LINK-RACE"); String path = linkPath(c, member) + "/changes"; String bearer = token(admin);
        String first = linkRequest("LINK", e.employeeId(), 0, 1, UUID.randomUUID());
        owner.execute("create function reject_link_audit() returns trigger language plpgsql as $$ begin raise exception 'synthetic link audit outage'; end $$");
        owner.execute("create trigger reject_link_audit before insert on employment_account_link_audit for each row execute function reject_link_audit()");
        try {
            assertThat(authenticatedPost(path, bearer, first).statusCode()).isEqualTo(500);
            assertThat(owner.queryForObject("select count(*) from employment_account_link where company_id = ?", Integer.class, c)).isZero();
        } finally { owner.execute("drop trigger reject_link_audit on employment_account_link_audit"); owner.execute("drop function reject_link_audit()"); }
        var same = concurrentPosts(path, bearer, first, first);
        assertThat(same).extracting(HttpResponse::statusCode).containsExactly(200, 200);
        assertThat(same.get(0).body()).isEqualTo(same.get(1).body());
        assertThat(authenticatedPost(linkPath(c, secondMember) + "/changes", bearer, linkRequest("LINK", e.employeeId(), 0, 1, UUID.randomUUID())).statusCode()).isEqualTo(409);
        var next = employeeEpisode(c, "EMP-LINK-NEXT");
        var race = concurrentPosts(path, bearer, linkRequest("RELINK", next.employeeId(), 1, 1, UUID.randomUUID()), linkRequest("REVOKE", null, 1, 1, UUID.randomUUID()));
        assertThat(race).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409);
        assertThat(owner.queryForObject("select count(*) from employment_account_link_audit where company_id = ?", Integer.class, c)).isEqualTo(2);
    }

    @Test void linkRuntimePoliciesHideForeignHistoryAndRejectForgedScope() throws Exception {
        UUID a = company(); UUID b = company(); String admin = subject(); String employee = subject();
        membership(admin, a, "ACTIVE", "identity.link.admin"); membership(admin, b, "ACTIVE", "identity.link.admin");
        UUID member = membership(employee, a, "ACTIVE", "employee.self.read");
        var e = employeeEpisode(a, "EMP-LINK-RLS");
        assertThat(authenticatedPost(linkPath(a, member) + "/changes", token(admin), linkRequest("LINK", e.employeeId(), 0, 1, UUID.randomUUID())).statusCode()).isEqualTo(200);
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link", Integer.class)).isZero();
            scope.apply(jdbc, admin, b);
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link_audit", Integer.class)).isZero();
            scope.apply(jdbc, employee, a);
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link where membership_id = ?", Integer.class, member)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link_audit", Integer.class)).isZero();
            jdbc.queryForObject("select set_config('olarize.subject', ?, true)", String.class, admin);
            assertThat(jdbc.queryForObject("select count(*) from employment_account_link", Integer.class)).isZero();
        });
        assertThat(jdbc.queryForObject("select count(*) from employment_account_link", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("update employment_account_link_audit set reason = 'tampered' where membership_id = ?", member))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private String contractPath(UUID companyId, EmployeeEpisode employee) {
        return "/api/v1/companies/" + companyId + "/employees/" + employee.employeeId()
                + "/episodes/" + employee.episodeId() + "/contracts";
    }

    private String createContractRequest(UUID key, String endDate) {
        return """
                {"typeCode":"CDD","startDate":"2026-01-01","endDate":"%s",
                 "sourceReference":"Synthetic contract archive item","idempotencyKey":"%s","confirm":true}
                """.formatted(endDate, key);
    }

    private String correctContractRequest(UUID key, long expectedVersion, String endDate, String reason) {
        return """
                {"expectedVersion":%d,"typeCode":"CDD","startDate":"2026-01-01","endDate":"%s",
                 "sourceReference":"Synthetic corrected contract source","reason":"%s",
                 "evidenceReference":"Synthetic amendment record","idempotencyKey":"%s","confirm":true}
                """.formatted(expectedVersion, endDate, reason, key);
    }

    private UUID contractFacts(UUID companyId, EmployeeEpisode employee, UUID contractId,
            java.time.LocalDate currentEndDate, java.time.LocalDate previousEndDate) {
        owner.update("insert into employment_contract(id,company_id,employee_id,episode_id,current_version,lock_version) values (?,?,?,?,2,0)",
                contractId, companyId, employee.employeeId(), employee.episodeId());
        for (int version = 1; version <= 2; version++) {
            owner.update("""
                    insert into employment_contract_version(id,contract_id,company_id,employee_id,episode_id,
                        version,type_code,start_date,end_date,source_reference,review_state,actor_subject,
                        operation,change_reason,evidence_reference,correlation_id,recorded_at)
                    values (?,?,?,?,?,?,'CDD',DATE '2026-01-01',?,'Synthetic expiry fixture','UNREVIEWED',
                        'test-fixture',?, ?, ?, 'expiry-test', now())
                    """, UUID.randomUUID(), contractId, companyId, employee.employeeId(), employee.episodeId(), version,
                    version == 1 ? previousEndDate : currentEndDate,
                    version == 1 ? "CREATE" : "CORRECT",
                    version == 1 ? null : "Corrected fixture date",
                    version == 1 ? null : "Synthetic correction evidence");
        }
        return contractId;
    }

    @Test void hrCreatesCorrectsAndRetriesVersionedContractFacts() throws Exception {
        UUID companyId = company();
        String hr = subject();
        membership(hr, companyId, "ACTIVE", "contract.write");
        EmployeeEpisode employee = employeeEpisode(companyId, "EMP-CONTRACT-1");
        String path = contractPath(companyId, employee);
        Integer contractsBefore = owner.queryForObject("select count(*) from employment_contract", Integer.class);
        UUID createKey = UUID.randomUUID();
        String createBody = createContractRequest(createKey, "2027-08-31");

        var created = authenticatedPost(path, token(hr), createBody);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        var initial = json(created.body());
        assertThat(initial.path("version").asLong()).isEqualTo(1);
        assertThat(initial.path("reviewState").asText()).isEqualTo("UNREVIEWED");
        assertThat(initial.path("endDateCompleteness").asText()).isEqualTo("RECORDED");
        assertThat(authenticatedPost(path, token(hr), createBody).body()).isEqualTo(created.body());
        assertThat(authenticatedPost(path, token(hr), createContractRequest(createKey, "2027-09-30"))
                .statusCode()).isEqualTo(409);

        UUID contractId = UUID.fromString(initial.path("contractId").asText());
        String correctionPath = path + "/" + contractId + "/corrections";
        UUID correctionKey = UUID.randomUUID();
        String correctionBody = correctContractRequest(correctionKey, 1, "2027-12-31", "Date copied incorrectly");
        var corrected = authenticatedPost(correctionPath, token(hr), correctionBody);
        assertThat(corrected.statusCode()).as(corrected.body()).isEqualTo(200);
        assertThat(json(corrected.body()).path("version").asLong()).isEqualTo(2);
        assertThat(json(corrected.body()).path("endDate").asText()).isEqualTo("2027-12-31");
        assertThat(json(corrected.body()).path("changeReason").asText()).isEqualTo("Date copied incorrectly");
        assertThat(authenticatedPost(correctionPath, token(hr), correctionBody).body()).isEqualTo(corrected.body());
        assertThat(authenticatedPost(correctionPath, token(hr),
                correctContractRequest(UUID.randomUUID(), 1, "2027-12-31", "Stale correction"))
                .statusCode()).isEqualTo(409);

        assertThat(owner.queryForObject("select count(*) from employment_contract where company_id = ?", Integer.class, companyId)).isEqualTo(1);
        assertThat(owner.queryForObject("select count(*) from employment_contract_version where contract_id = ?", Integer.class, contractId)).isEqualTo(2);
        assertThat(owner.queryForObject("select end_date from employment_contract_version where contract_id = ? and version = 1", java.time.LocalDate.class, contractId))
                .isEqualTo(java.time.LocalDate.of(2027, 8, 31));
        assertThat(owner.queryForObject("select current_version from employment_contract where id = ?", Long.class, contractId)).isEqualTo(2);
        assertThat(owner.queryForObject("select count(*) from employment_contract", Integer.class))
                .isEqualTo(contractsBefore + 1);
        new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()))
                .execute(status -> {
                    scope.apply(jdbc, hr, companyId);
                    assertThat(jdbc.queryForObject("select count(*) from employment_contract where id = ?", Integer.class, contractId)).isEqualTo(1);
                    return null;
                });
        assertThat(owner.queryForObject("select last_employed_day from employment_episode where id = ?", java.time.LocalDate.class, employee.episodeId()))
                .isNull();
    }

    @Test void contractWritesRequireCurrentCompanyPermissionAndRejectForeignOrInvalidFacts() throws Exception {
        UUID companyA = company(); UUID companyB = company();
        String hrA = subject(); String hrB = subject(); String ordinary = subject();
        UUID hrMembership = membership(hrA, companyA, "ACTIVE", "contract.write");
        membership(hrB, companyB, "ACTIVE", "contract.write");
        membership(ordinary, companyA, "ACTIVE", "contract.read");
        EmployeeEpisode employeeA = employeeEpisode(companyA, "EMP-CONTRACT-A");
        EmployeeEpisode employeeB = employeeEpisode(companyB, "EMP-CONTRACT-B");
        String pathA = contractPath(companyA, employeeA);
        String pathB = contractPath(companyB, employeeB);

        assertThat(authenticatedPost(pathA, token(ordinary), createContractRequest(UUID.randomUUID(), "2027-01-01")).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(pathB, token(hrA), createContractRequest(UUID.randomUUID(), "2027-01-01")).statusCode()).isEqualTo(403);
        String foreignPath = "/api/v1/companies/" + companyA + "/employees/" + employeeB.employeeId()
                + "/episodes/" + employeeB.episodeId() + "/contracts";
        assertThat(authenticatedPost(foreignPath, token(hrA), createContractRequest(UUID.randomUUID(), "2027-01-01")).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(pathA, token(hrA), createContractRequest(UUID.randomUUID(), "2025-12-31")).statusCode()).isEqualTo(400);
        String invalidCode = createContractRequest(UUID.randomUUID(), "2027-01-01").replace("\"typeCode\":\"CDD\"", "\"typeCode\":\"LEGAL_VERDICT\"");
        var invalid = authenticatedPost(pathA, token(hrA), invalidCode);
        assertThat(invalid.statusCode()).isEqualTo(400);
        String unsupportedOtherCode = createContractRequest(UUID.randomUUID(), "2027-01-01")
                .replace("\"typeCode\":\"CDD\"", "\"typeCode\":\"OTHER\"");
        assertThat(authenticatedPost(pathA, token(hrA), unsupportedOtherCode).statusCode()).isEqualTo(400);
        assertThat(invalid.body()).doesNotContain("LEGAL_VERDICT");
        assertThat(owner.queryForObject("select count(*) from employment_contract", Integer.class)).isZero();
        owner.update("update identity_membership set status = 'REVOKED', version = version + 1 where id = ?", hrMembership);
        assertThat(authenticatedPost(pathA, token(hrA), createContractRequest(UUID.randomUUID(), "2027-01-01")).statusCode()).isEqualTo(403);
    }

    @Test void hrSearchesDuplicateNamesStatusesAndScopedDetails() throws Exception {
        UUID companyA = company(); UUID companyB = company();
        String hr = subject(); String ordinary = subject();
        UUID hrMembership = membership(hr, companyA, "ACTIVE", "employee.read");
        membership(ordinary, companyA, "ACTIVE", "employee.self.read");
        var first = employeeEpisode(companyA, "EMP-0001");
        var second = employeeEpisode(companyA, "EMP-0002");
        var ended = employeeEpisode(companyA, "EMP-0003");
        var upcoming = employeeEpisode(companyA, "EMP-0004");
        var foreign = employeeEpisode(companyB, "EMP-0001");
        owner.update("update employment_employee set name = 'Ahmed El Amrani' where id in (?, ?)", first.employeeId(), second.employeeId());
        owner.update("update employment_episode set last_employed_day = DATE '2026-09-29' where id = ?", ended.episodeId());
        owner.update("update employment_episode set start_date = DATE '2026-10-01' where id = ?", upcoming.episodeId());
        // A later episode must not duplicate an employee or replace the episode relevant to asOf.
        owner.update("insert into employment_episode(id, employee_id, company_id, start_date, recorded_at) values (?, ?, ?, DATE '2027-01-01', now())",
                UUID.randomUUID(), first.employeeId(), companyA);
        String base = "/api/v1/companies/" + companyA + "/employees";
        String bearer = token(hr);
        var page = authenticatedGet(base + "?asOf=2026-09-30&search=ahmed&pageSize=1", bearer);
        assertThat(page.statusCode()).as(page.body()).isEqualTo(200);
        var body = json(page.body());
        assertThat(body.path("items").get(0).path("reference").asText()).isEqualTo("EMP-0001");
        assertThat(body.path("items").get(0).path("episodeId").asText()).isEqualTo(first.episodeId().toString());
        assertThat(body.path("pagination").path("totalItems").asLong()).isEqualTo(2);
        assertThat(page.headers().firstValue("Cache-Control")).hasValue("no-store");
        var next = authenticatedGet(base + "?asOf=2026-09-30&search=Ahmed&pageSize=1&page=1", bearer);
        assertThat(json(next.body()).path("items").get(0).path("reference").asText()).isEqualTo("EMP-0002");
        assertThat(json(authenticatedGet(base + "?asOf=2026-09-30&status=ALL", bearer).body())
                .path("pagination").path("totalItems").asLong()).isEqualTo(4);
        assertThat(json(authenticatedGet(base + "?asOf=2026-09-30&status=ENDED", bearer).body())
                .path("items").get(0).path("employeeId").asText()).isEqualTo(ended.employeeId().toString());
        assertThat(json(authenticatedGet(base + "?asOf=2026-09-30&status=UPCOMING", bearer).body())
                .path("items").get(0).path("employeeId").asText()).isEqualTo(upcoming.employeeId().toString());
        assertThat(json(authenticatedGet(base + "?asOf=2026-09-29&status=CURRENT", bearer).body())
                .path("pagination").path("totalItems").asLong()).isEqualTo(3);
        assertThat(json(authenticatedGet(base + "?asOf=2026-10-01&status=UPCOMING", bearer).body()).path("items").isEmpty()).isTrue();
        assertThat(json(authenticatedGet(base + "?asOf=2026-09-30&search=EMP-0002", bearer).body())
                .path("items").get(0).path("employeeId").asText()).isEqualTo(second.employeeId().toString());
        assertThat(json(authenticatedGet(base + "?search=%25", bearer).body()).path("items").isEmpty()).isTrue();
        var detail = authenticatedGet(base + "/" + first.employeeId() + "?asOf=2026-09-30", bearer);
        assertThat(detail.statusCode()).as(detail.body()).isEqualTo(200);
        assertThat(json(detail.body()).path("name").asText()).isEqualTo("Ahmed El Amrani");
        assertThat(authenticatedGet(base + "/" + foreign.employeeId(), bearer).statusCode()).isEqualTo(404);
        assertThat(authenticatedGet(base + "/" + UUID.randomUUID(), bearer).statusCode()).isEqualTo(404);
        assertThat(authenticatedGet(base.replace(companyA.toString(), companyB.toString()), bearer).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet(base, token(ordinary)).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet(base + "/" + first.employeeId(), token(ordinary)).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet(base, "invalid-token").statusCode()).isEqualTo(401);
        for (String query : List.of("page=-1", "pageSize=101", "pageSize=0", "page=2147483647&pageSize=100", "status=UNKNOWN", "asOf=bad", "unknown=true", "page=0&page=1")) {
            var invalid = authenticatedGet(base + "?" + query, bearer);
            assertThat(invalid.statusCode()).as(query).isEqualTo(400);
            var problem = json(invalid.body());
            assertThat(problem.path("status").asInt()).isEqualTo(400);
            assertThat(problem.path("requestId").asText()).isNotBlank();
            assertThat(invalid.body()).doesNotContain("2147483647", "Hibernate", "SQL");
        }
        var transactions = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        assertThat(jdbc.queryForObject("select count(*) from employment_employee", Integer.class)).isZero();
        transactions.execute(status -> {
            jdbc.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, companyA.toString());
            assertThat(jdbc.queryForObject("select count(*) from employment_employee", Integer.class)).isZero();
            scope.apply(jdbc, hr, companyA);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee", Integer.class)).isEqualTo(4);
            assertThat(jdbc.queryForObject("select count(*) from employment_employee where company_id = ?", Integer.class, companyB)).isZero();
            return null;
        });
        owner.update("update identity_membership set status = 'REVOKED' where id = ?", hrMembership);
        assertThat(authenticatedGet(base, bearer).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet(base + "/" + first.employeeId(), bearer).statusCode()).isEqualTo(403);
    }

    @Test void directoryDatabaseFailureReturnsProblemInsteadOfEmptySuccess() throws Exception {
        UUID companyId = company(); String hr = subject();
        membership(hr, companyId, "ACTIVE", "employee.read");
        owner.execute("revoke select on employment_employee from membership_runtime");
        try {
            var response = authenticatedGet("/api/v1/companies/" + companyId + "/employees", token(hr));
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(json(response.body()).path("requestId").asText()).isNotBlank();
            assertThat(response.body()).doesNotContain("permission denied", "employment_employee", "select ");
        } finally {
            owner.execute("grant select on employment_employee to membership_runtime");
        }
    }

    @Test void expiryAssistantReturnsActualRecordsAndEnforcesConversationOwnership() throws Exception {
        UUID companyA = company(); UUID companyB = company();
        String hr = subject(); String otherHr = subject(); String employee = subject();
        membership(hr, companyA, "ACTIVE", "contract.read");
        membership(hr, companyB, "ACTIVE", "contract.read");
        membership(otherHr, companyA, "ACTIVE", "contract.read");
        membership(employee, companyA, "ACTIVE", "employee.self.read");
        EmployeeEpisode employeeA = employeeEpisode(companyA, "EMP-TOOL-A");
        EmployeeEpisode employeeB = employeeEpisode(companyB, "EMP-TOOL-B");
        UUID contract = UUID.randomUUID();
        var next = java.time.YearMonth.now(java.time.ZoneId.of("Africa/Casablanca")).plusMonths(1);
        contractFacts(companyA, employeeA, contract, next.atDay(15), null);
        contractFacts(companyB, employeeB, UUID.randomUUID(), next.atDay(15), null);
        String base = "/api/v1/companies/" + companyA + "/assistant/conversations";
        var created = authenticatedPost(base, token(hr), "");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.headers().firstValue("Cache-Control")).hasValue("no-store");
        String id = json(created.body()).path("conversationId").asText();
        String ask = base + "/" + id + "/contract-expiry-requests";
        for (String question : List.of("Which contracts expire next month?", "Quels contrats expirent le mois prochain ?", "ما هي العقود التي تنتهي الشهر المقبل؟")) {
            String body = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("question", question));
            var response = authenticatedPost(ask, token(hr), body);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            var result = json(response.body());
            assertThat(result.path("status").asText()).isEqualTo("RESULTS");
            assertThat(result.path("result").path("fromDate").asText()).isEqualTo(next.atDay(1).toString());
            assertThat(result.path("result").path("toDate").asText()).isEqualTo(next.atEndOfMonth().toString());
            assertThat(result.path("result").path("totalItems").asInt()).isEqualTo(1);
            assertThat(result.path("result").path("items").get(0).path("contractId").asText()).isEqualTo(contract.toString());
            assertThat(result.path("result").path("items").get(0).path("employeeReference").asText()).isEqualTo("EMP-TOOL-A");
            assertThat(response.body()).doesNotContain("EMP-TOOL-B");
        }
        var scopedTransaction = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        scopedTransaction.executeWithoutResult(status -> {
            scope.apply(jdbc, hr, companyA);
            assertThat(jdbc.queryForObject("select count(*) from assistant_conversation where id = ?", Integer.class, UUID.fromString(id))).isEqualTo(1);
            scope.apply(jdbc, otherHr, companyA);
            assertThat(jdbc.queryForObject("select count(*) from assistant_conversation where id = ?", Integer.class, UUID.fromString(id))).isZero();
            scope.apply(jdbc, hr, companyB);
            assertThat(jdbc.queryForObject("select count(*) from assistant_conversation where id = ?", Integer.class, UUID.fromString(id))).isZero();
            jdbc.queryForObject("select set_config('olarize.subject', ?, true)", String.class, otherHr);
            assertThat(jdbc.queryForObject("select count(*) from assistant_conversation", Integer.class)).isZero();
        });
        String body = "{\"question\":\"Which contracts expire next month?\"}";
        assertThat(authenticatedPost(ask, token(otherHr), body).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(ask.replace(companyA.toString(), companyB.toString()), token(hr), body).statusCode()).isEqualTo(404);
        assertThat(authenticatedPost(ask, token(employee), body).statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(base, token(employee), "").statusCode()).isEqualTo(403);
        assertThat(authenticatedPost(ask, "invalid-token", body).statusCode()).isEqualTo(401);
        assertThat(authenticatedPost(ask, token(hr), "{\"question\":\"Which contracts expire next month?\",\"userId\":\"other\"}").statusCode()).isEqualTo(400);
        assertThat(authenticatedPost(ask, token(hr), "{\"question\":\"Which contracts expire next month?\",\"page\":2147483647,\"pageSize\":100}").statusCode()).isEqualTo(400);
        var clarify = authenticatedPost(ask, token(hr), "{\"question\":\"Which contracts expire in October?\"}");
        assertThat(clarify.statusCode()).isEqualTo(200);
        assertThat(json(clarify.body()).path("status").asText()).isEqualTo("CLARIFICATION_REQUIRED");
        assertThat(json(clarify.body()).path("result").isNull()).isTrue();
        var unavailable = authenticatedPost(ask, token(hr), "{\"question\":\"simulate provider failure\"}");
        assertThat(unavailable.statusCode()).isEqualTo(503);
        assertThat(json(unavailable.body()).path("requestId").asText()).isNotBlank();
        owner.update("update assistant_conversation set created_at = now() - interval '2 days', expires_at = now() - interval '1 day' where id = ?", UUID.fromString(id));
        assertThat(authenticatedPost(ask, token(hr), body).statusCode()).isEqualTo(404);
        var fresh = authenticatedPost(base, token(hr), "");
        String freshId = json(fresh.body()).path("conversationId").asText();
        owner.update("update identity_membership set status = 'REVOKED' where company_id = ? and subject = ?", companyA, hr);
        assertThat(authenticatedPost(base + "/" + freshId + "/contract-expiry-requests", token(hr), body).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select count(*) from assistant_conversation", Integer.class)).isZero();
    }

    @Test void assistantEmptyAndToolFailureAreDifferentResponses() throws Exception {
        UUID companyId = company(); String hr = subject();
        membership(hr, companyId, "ACTIVE", "contract.read");
        String base = "/api/v1/companies/" + companyId + "/assistant/conversations";
        var created = authenticatedPost(base, token(hr), "");
        String ask = base + "/" + json(created.body()).path("conversationId").asText() + "/contract-expiry-requests";
        String body = "{\"question\":\"Which contracts expire next month?\"}";
        var empty = authenticatedPost(ask, token(hr), body);
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json(empty.body()).path("status").asText()).isEqualTo("EMPTY");
        assertThat(json(empty.body()).path("result").path("totalItems").asInt()).isZero();
        owner.execute("revoke select on employment_contract_version from membership_runtime");
        try {
            var unavailable = authenticatedPost(ask, token(hr), body);
            assertThat(unavailable.statusCode()).isEqualTo(500);
            assertThat(json(unavailable.body()).path("requestId").asText()).isNotBlank();
            assertThat(unavailable.body()).doesNotContain("employment_contract", "select ", "permission denied");
        } finally { owner.execute("grant select on employment_contract_version to membership_runtime"); }
    }

    @Test void expiryListUsesCurrentVersionsInclusiveDatesStablePagesAndCompanyScopedCoverage() throws Exception {
        UUID companyA = company(); UUID companyB = company();
        String hrA = subject(); String payrollUser = subject();
        membership(hrA, companyA, "ACTIVE", "contract.read");
        membership(payrollUser, companyA, "ACTIVE", "payroll.roster.read");
        EmployeeEpisode employeeA = employeeEpisode(companyA, "EMP-EXPIRY-A");
        EmployeeEpisode employeeB = employeeEpisode(companyB, "EMP-EXPIRY-B");
        UUID firstContract = UUID.fromString("00000000-0000-4000-8000-000000000101");
        UUID secondContract = UUID.fromString("00000000-0000-4000-8000-000000000102");
        contractFacts(companyA, employeeA, firstContract, java.time.LocalDate.of(2027, 8, 31),
                java.time.LocalDate.of(2027, 7, 31));
        contractFacts(companyA, employeeA, secondContract, java.time.LocalDate.of(2027, 8, 31),
                java.time.LocalDate.of(2027, 8, 31));
        contractFacts(companyA, employeeA, UUID.randomUUID(), java.time.LocalDate.of(2027, 8, 30), null);
        contractFacts(companyA, employeeA, UUID.randomUUID(), java.time.LocalDate.of(2027, 9, 1), null);
        contractFacts(companyA, employeeA, UUID.randomUUID(), null, null);
        contractFacts(companyB, employeeB, UUID.randomUUID(), java.time.LocalDate.of(2027, 8, 31), null);
        contractFacts(companyB, employeeB, UUID.randomUUID(), null, null);

        String base = "/api/v1/companies/" + companyA + "/contracts/expiring?fromDate=2027-08-31&toDate=2027-08-31&pageSize=1";
        var firstPage = authenticatedGet(base, token(hrA));
        assertThat(firstPage.statusCode()).as(firstPage.body()).isEqualTo(200);
        var firstPageJson = json(firstPage.body());
        assertThat(firstPageJson.path("items").size()).isEqualTo(1);
        assertThat(firstPageJson.path("items").get(0).path("contractId").asText()).isEqualTo(firstContract.toString());
        assertThat(firstPageJson.path("items").get(0).path("contractVersion").asLong()).isEqualTo(2);
        assertThat(firstPageJson.path("pagination").path("totalItems").asLong()).isEqualTo(2);
        assertThat(firstPageJson.path("pagination").path("totalPages").asInt()).isEqualTo(2);
        assertThat(firstPageJson.path("currentContractsWithoutRecordedEndDate").asLong()).isEqualTo(1);
        assertThat(firstPageJson.path("items").get(0).path("employeeReference").asText()).isEqualTo("EMP-EXPIRY-A");

        var secondPage = authenticatedGet(base + "&page=1", token(hrA));
        assertThat(secondPage.statusCode()).isEqualTo(200);
        assertThat(json(secondPage.body()).path("items").get(0).path("contractId").asText())
                .isEqualTo(secondContract.toString());
        var empty = authenticatedGet(base.replace("2027-08-31", "2027-09-02"), token(hrA));
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json(empty.body()).path("items").isEmpty()).isTrue();
        assertThat(json(empty.body()).path("pagination").path("totalItems").asLong()).isZero();
        assertThat(authenticatedGet(base, token(payrollUser)).statusCode()).isEqualTo(403);
        assertThat(authenticatedGet(base.replace(companyA.toString(), companyB.toString()), token(hrA)).statusCode())
                .isEqualTo(403);
        assertThat(authenticatedGet(base + "&unknown=true", token(hrA)).statusCode()).isEqualTo(400);
        assertThat(authenticatedGet(base + "&pageSize=101", token(hrA)).statusCode()).isEqualTo(400);
        assertThat(authenticatedGet(base, "invalid-token").statusCode()).isEqualTo(401);
    }

    @Test void contractVersionAuditFailureRollsBackAndSameRequestCanRetry() throws Exception {
        UUID companyId = company(); String hr = subject();
        membership(hr, companyId, "ACTIVE", "contract.write");
        EmployeeEpisode employee = employeeEpisode(companyId, "EMP-CONTRACT-ROLLBACK");
        String path = contractPath(companyId, employee);
        String body = createContractRequest(UUID.randomUUID(), "2027-01-01");
        owner.execute("""
                create function reject_contract_version() returns trigger language plpgsql as $$
                begin raise exception 'synthetic contract audit outage'; end; $$
                """);
        owner.execute("create trigger reject_contract_version before insert on employment_contract_version for each row execute function reject_contract_version()");
        try {
            assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(500);
        } finally {
            owner.execute("drop trigger reject_contract_version on employment_contract_version");
            owner.execute("drop function reject_contract_version()");
        }
        assertThat(owner.queryForObject("select count(*) from employment_contract where company_id = ?", Integer.class, companyId)).isZero();
        assertThat(owner.queryForObject("select count(*) from employment_contract_request where company_id = ?", Integer.class, companyId)).isZero();
        assertThat(authenticatedPost(path, token(hr), body).statusCode()).isEqualTo(201);
    }

    @Test void simultaneousCorrectionsWithTheSameVersionAllowOnlyOneChange() throws Exception {
        UUID companyId = company(); String hr = subject();
        membership(hr, companyId, "ACTIVE", "contract.write");
        EmployeeEpisode employee = employeeEpisode(companyId, "EMP-CONTRACT-RACE");
        String path = contractPath(companyId, employee);
        var created = authenticatedPost(path, token(hr), createContractRequest(UUID.randomUUID(), "2027-01-01"));
        UUID contractId = UUID.fromString(json(created.body()).path("contractId").asText());
        String correctionPath = path + "/" + contractId + "/corrections";
        String first = correctContractRequest(UUID.randomUUID(), 1, "2027-02-01", "Correction A");
        String second = correctContractRequest(UUID.randomUUID(), 1, "2027-03-01", "Correction B");
        var responses = concurrentPosts(correctionPath, token(hr), first, second);
        assertThat(responses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409);
        assertThat(owner.queryForObject("select count(*) from employment_contract_version where contract_id = ?", Integer.class, contractId)).isEqualTo(2);
        assertThat(owner.queryForObject("select current_version from employment_contract where id = ?", Long.class, contractId)).isEqualTo(2);
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

    @Test void identityAdministratorCanPreviewGrantConfirmAndRetryWithAtomicAudit() throws Exception {
        IDENTITY_PROVIDER_UNAVAILABLE.set(false);
        String actor = subject(); String target = subject(); var company = company();
        membership(actor, company, "ACTIVE", "identity.admin");
        String actorToken = token(actor);
        String base = "/api/v1/companies/" + company;
        String previewBody = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "userId", target, "roleBundle", "HR_ADMIN", "reason", "Synthetic onboarding"));

        var previewResponse = authenticatedPost(base + "/membership-change-previews", actorToken, previewBody);
        assertThat(previewResponse.statusCode()).isEqualTo(201);
        assertThat(previewResponse.body()).contains(target, "HR_ADMIN", "employee.read", "contract.write")
                .doesNotContain("payroll.read", "payroll.roster.read");
        assertThat(owner.queryForObject("select count(*) from identity_membership where company_id=? and subject=?", Integer.class, company, target)).isZero();

        var preview = json(previewResponse.body());
        assertThat(preview.get("userId").stringValue()).isEqualTo(target);
        assertThat(preview.has("targetSubject")).isFalse();
        String confirmation = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", preview.get("previewToken").stringValue(), "expectedTargetVersion", 0,
                "idempotencyKey", java.util.UUID.randomUUID().toString(), "confirm", true));
        var first = authenticatedPost(base + "/membership-changes/confirm", actorToken, confirmation);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains(target, "ACTIVE", "HR_ADMIN", "employee.read", "contract.write")
                .doesNotContain("payroll.read");
        var confirmed = json(first.body());
        assertThat(confirmed.get("userId").stringValue()).isEqualTo(target);
        assertThat(confirmed.has("subject")).isFalse();
        String id = confirmed.get("membershipId").stringValue();
        var audit = owner.queryForMap("select * from identity_membership_change_audit where membership_id=?", java.util.UUID.fromString(id));
        assertThat(audit).containsEntry("actor_subject", actor).containsEntry("action", "GRANT")
                .containsEntry("previous_status", null).containsEntry("next_status", "ACTIVE")
                .containsEntry("result_version", 1L).containsEntry("reason", "Synthetic onboarding");
        assertThat((String[]) ((java.sql.Array) audit.get("next_permissions")).getArray()).contains("employee.read", "contract.write")
                .doesNotContain("payroll.read", "identity.admin");

        IDENTITY_PROVIDER_UNAVAILABLE.set(true);
        HttpResponse<String> retry;
        try { retry = authenticatedPost(base + "/membership-changes/confirm", actorToken, confirmation); }
        finally { IDENTITY_PROVIDER_UNAVAILABLE.set(false); }
        assertThat(retry.statusCode()).isEqualTo(200);
        assertThat(json(retry.body()).get("membershipId").stringValue()).isEqualTo(id);
        assertThat(owner.queryForObject("select count(*) from identity_membership_change_audit where membership_id=?", Integer.class,
                java.util.UUID.fromString(id))).isEqualTo(1);
        assertThat(authenticatedGet("/api/v1/identity/company-context", token(target)).body()).contains("employee.read", "contract.write");

        String otherTarget = subject();
        var otherPreview = authenticatedPost(base + "/membership-change-previews", actorToken,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", otherTarget,
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic conflicting retry")));
        var otherClaims = json(otherPreview.body());
        String conflicting = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", otherClaims.get("previewToken").stringValue(), "expectedTargetVersion", 0,
                "idempotencyKey", json(confirmation).get("idempotencyKey").stringValue(), "confirm", true));
        assertThat(authenticatedPost(base + "/membership-changes/confirm", actorToken, conflicting).statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select count(*) from identity_membership where company_id=? and subject=?",
                Integer.class, company, otherTarget)).isZero();
    }

    @Test void membershipAdministrationIsCompanyScopedAndRejectsSelfOrProtectedTargets() throws Exception {
        String actor = subject(); String ordinary = subject(); String foreignAdmin = subject();
        var companyA = company(); var companyB = company();
        membership(actor, companyA, "ACTIVE", "identity.admin");
        membership(ordinary, companyA, "ACTIVE", "employee.self.read");
        membership(foreignAdmin, companyB, "ACTIVE", "identity.admin");
        String token = token(actor); String base = "/api/v1/companies/" + companyA;
        var listed = authenticatedGet(base + "/memberships", token);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(listed.body()).contains(actor, ordinary).doesNotContain(foreignAdmin, companyB.toString());
        var rows = json(listed.body());
        boolean foundOrdinary = false;
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            assertThat(row.has("userId")).isTrue();
            assertThat(row.has("subject")).isFalse();
            if (ordinary.equals(row.get("userId").stringValue())) foundOrdinary = true;
        }
        assertThat(foundOrdinary).isTrue();
        assertThat(authenticatedGet("/api/v1/companies/" + companyB + "/memberships", token).statusCode()).isEqualTo(403);

        var ordinaryActor = authenticatedPost(base + "/membership-change-previews", token(ordinary),
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", subject(),
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic reason")));
        assertThat(ordinaryActor.statusCode()).isEqualTo(403);
        var selfEdit = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", actor,
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic reason")));
        assertThat(selfEdit.statusCode()).isEqualTo(409);
        var foreignReference = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", foreignAdmin,
                        "reason", "Synthetic revoke")));
        assertThat(foreignReference.statusCode()).isEqualTo(409);
        assertThat(foreignReference.body()).doesNotContain(foreignAdmin, companyB.toString());

        var protectedAdmin = subject(); membership(protectedAdmin, companyA, "ACTIVE", "identity.admin");
        var editAdmin = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", protectedAdmin,
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic reason")));
        assertThat(editAdmin.statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select count(*) from identity_membership_change_audit where company_id=?", Integer.class, companyA)).isZero();
    }

    @Test void roleChangeAppliesReducedPermissionsOnTheTargetsNextRequest() throws Exception {
        String actor = subject(); String target = subject(); var company = company();
        membership(actor, company, "ACTIVE", "identity.admin");
        var targetMembership = membership(target, company, "ACTIVE", "employee.self.read");
        owner.update("update identity_membership set role_bundle='EMPLOYEE' where id=?", targetMembership);
        String targetToken = token(target); String actorToken = token(actor);
        String context = "/api/v1/identity/company-context?companyId=" + company;
        assertThat(authenticatedGet(context, targetToken).body()).contains("employee.self.read");

        String base = "/api/v1/companies/" + company;
        var preview = authenticatedPost(base + "/membership-change-previews", actorToken,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", target,
                        "roleBundle", "MANAGER", "reason", "Synthetic role change")));
        assertThat(preview.statusCode()).isEqualTo(201);
        assertThat(preview.body()).contains("UPDATE", "employee.direct-reports.read").doesNotContain("employee.self.read");
        String confirmation = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", json(preview.body()).get("previewToken").stringValue(), "expectedTargetVersion", 1,
                "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
        var changed = authenticatedPost(base + "/membership-changes/confirm", actorToken, confirmation);
        assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(changed.body()).contains("MANAGER", "employee.direct-reports.read").doesNotContain("employee.self.read");
        assertThat(authenticatedGet(context, targetToken).body()).contains("employee.direct-reports.read")
                .doesNotContain("employee.self.read");

        var audit = owner.queryForMap("select * from identity_membership_change_audit where membership_id=?", targetMembership);
        assertThat(audit).containsEntry("action", "UPDATE").containsEntry("previous_role_bundle", "EMPLOYEE")
                .containsEntry("next_role_bundle", "MANAGER").containsEntry("previous_status", "ACTIVE")
                .containsEntry("next_status", "ACTIVE").containsEntry("expected_target_version", 1L)
                .containsEntry("result_version", 2L);
        assertThat((String[]) ((java.sql.Array) audit.get("previous_permissions")).getArray()).contains("employee.self.read");
        assertThat((String[]) ((java.sql.Array) audit.get("next_permissions")).getArray())
                .contains("employee.direct-reports.read").doesNotContain("employee.self.read");
    }

    @Test void reactivationRequiresVerifiedSubjectAndRecordsBeforeAfterAudit() throws Exception {
        String actor = subject(); String target = subject(); var company = company();
        membership(actor, company, "ACTIVE", "identity.admin");
        var targetMembership = membership(target, company, "REVOKED", "employee.self.read");
        owner.update("update identity_membership set role_bundle='EMPLOYEE' where id=?", targetMembership);
        String actorToken = token(actor); String targetToken = token(target);
        String base = "/api/v1/companies/" + company;
        String context = "/api/v1/identity/company-context?companyId=" + company;
        assertThat(authenticatedGet(context, targetToken).statusCode()).isEqualTo(403);

        String previewBody = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", target,
                "roleBundle", "MANAGER", "reason", "Synthetic reactivation"));
        IDENTITY_PROVIDER_UNAVAILABLE.set(true);
        HttpResponse<String> unavailable;
        try { unavailable = authenticatedPost(base + "/membership-change-previews", actorToken, previewBody); }
        finally { IDENTITY_PROVIDER_UNAVAILABLE.set(false); }
        assertThat(unavailable.statusCode()).isEqualTo(503);
        assertThat(owner.queryForObject("select status from identity_membership where id=?", String.class, targetMembership)).isEqualTo("REVOKED");
        assertThat(owner.queryForObject("select count(*) from identity_membership_change_audit where membership_id=?", Integer.class, targetMembership)).isZero();

        var preview = authenticatedPost(base + "/membership-change-previews", actorToken, previewBody);
        assertThat(preview.statusCode()).isEqualTo(201);
        assertThat(preview.body()).contains("REACTIVATE", "employee.direct-reports.read");
        String confirmation = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", json(preview.body()).get("previewToken").stringValue(), "expectedTargetVersion", 1,
                "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
        var reactivated = authenticatedPost(base + "/membership-changes/confirm", actorToken, confirmation);
        assertThat(reactivated.statusCode()).isEqualTo(200);
        assertThat(reactivated.body()).contains("ACTIVE", "MANAGER", "employee.direct-reports.read")
                .doesNotContain("employee.self.read");
        assertThat(authenticatedGet(context, targetToken).body()).contains("employee.direct-reports.read")
                .doesNotContain("employee.self.read");

        var audit = owner.queryForMap("select * from identity_membership_change_audit where membership_id=?", targetMembership);
        assertThat(audit).containsEntry("action", "REACTIVATE").containsEntry("previous_role_bundle", "EMPLOYEE")
                .containsEntry("next_role_bundle", "MANAGER").containsEntry("previous_status", "REVOKED")
                .containsEntry("next_status", "ACTIVE").containsEntry("expected_target_version", 1L)
                .containsEntry("result_version", 2L);
        assertThat((String[]) ((java.sql.Array) audit.get("previous_permissions")).getArray()).contains("employee.self.read");
        assertThat((String[]) ((java.sql.Array) audit.get("next_permissions")).getArray())
                .contains("employee.direct-reports.read").doesNotContain("employee.self.read");
    }

    @Test void providerOutageBlocksGrantButDoesNotBlockRevocation() throws Exception {
        String actor = subject(); String existing = subject(); String newTarget = subject(); var company = company();
        membership(actor, company, "ACTIVE", "identity.admin");
        var existingId = membership(existing, company, "ACTIVE", "employee.self.read");
        String token = token(actor); String base = "/api/v1/companies/" + company;
        IDENTITY_PROVIDER_UNAVAILABLE.set(true);
        try {
            var grant = authenticatedPost(base + "/membership-change-previews", token,
                    new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", newTarget,
                            "roleBundle", "EMPLOYEE", "reason", "Synthetic grant")));
            assertThat(grant.statusCode()).isEqualTo(503);
            var revokePreview = authenticatedPost(base + "/membership-change-previews", token,
                    new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", existing,
                            "reason", "Synthetic revocation")));
            assertThat(revokePreview.statusCode()).isEqualTo(201);
            var preview = json(revokePreview.body());
            String confirmation = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                    "previewToken", preview.get("previewToken").stringValue(), "expectedTargetVersion", 1,
                    "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
            var revoked = authenticatedPost(base + "/membership-changes/confirm", token, confirmation);
            assertThat(revoked.statusCode()).isEqualTo(200);
            assertThat(revoked.body()).contains("REVOKED");
            assertThat(authenticatedGet("/api/v1/identity/company-context?companyId=" + company, token(existing)).statusCode()).isEqualTo(403);
            assertThat(owner.queryForObject("select status from identity_membership where id=?", String.class, existingId)).isEqualTo("REVOKED");
        } finally {
            IDENTITY_PROVIDER_UNAVAILABLE.set(false);
        }
        var missingSubject = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", "not-found",
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic missing subject")));
        assertThat(missingSubject.statusCode()).isEqualTo(409);
        assertThat(owner.queryForObject("select count(*) from identity_membership where company_id=? and subject='not-found'",
                Integer.class, company)).isZero();
    }

    @Test void concurrentGrantsForTheSameSubjectCreateOneMembershipAndAudit() throws Exception {
        IDENTITY_PROVIDER_UNAVAILABLE.set(false);
        String actor = subject(); String target = subject(); var company = company();
        String token = token(actor); String base = "/api/v1/companies/" + company;
        membership(actor, company, "ACTIVE", "identity.admin");
        var firstPreview = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", target,
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic concurrent grant")));
        var secondPreview = authenticatedPost(base + "/membership-change-previews", token,
                new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", target,
                        "roleBundle", "EMPLOYEE", "reason", "Synthetic concurrent grant")));
        String firstBody = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", json(firstPreview.body()).get("previewToken").stringValue(), "expectedTargetVersion", 0,
                "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
        String secondBody = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                "previewToken", json(secondPreview.body()).get("previewToken").stringValue(), "expectedTargetVersion", 0,
                "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
        var firstRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + base + "/membership-changes/confirm"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(firstBody)).build();
        var secondRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + base + "/membership-changes/confirm"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(secondBody)).build();
        var first = client.sendAsync(firstRequest, HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(secondRequest, HttpResponse.BodyHandlers.ofString());
        var responses = List.of(first.join(), second.join());
        assertThat(responses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409);
        assertThat(owner.queryForObject("select count(*) from identity_membership where company_id=? and subject=? and status='ACTIVE'",
                Integer.class, company, target)).isEqualTo(1);
        assertThat(owner.queryForObject("select count(*) from identity_membership_change_audit where company_id=? and subject=?",
                Integer.class, company, target)).isEqualTo(1);
    }

    @Test void auditFailureRollsBackMembershipMutation() throws Exception {
        String actor = subject(); String target = "synthetic-audit-failure"; var company = company();
        membership(actor, company, "ACTIVE", "identity.admin");
        owner.execute("""
                CREATE OR REPLACE FUNCTION public.fail_synthetic_membership_audit() RETURNS trigger
                LANGUAGE plpgsql AS $$ BEGIN
                    IF NEW.subject = 'synthetic-audit-failure' THEN RAISE EXCEPTION 'synthetic audit failure'; END IF;
                    RETURN NEW;
                END $$
                """);
        owner.execute("CREATE TRIGGER fail_synthetic_membership_audit BEFORE INSERT ON identity_membership_change_audit " +
                "FOR EACH ROW EXECUTE FUNCTION public.fail_synthetic_membership_audit()");
        try {
            String base = "/api/v1/companies/" + company; String token = token(actor);
            var previewResponse = authenticatedPost(base + "/membership-change-previews", token,
                    new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("userId", target,
                            "roleBundle", "EMPLOYEE", "reason", "Synthetic audit rollback")));
            var claims = json(previewResponse.body());
            String confirmation = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of(
                    "previewToken", claims.get("previewToken").stringValue(), "expectedTargetVersion", 0,
                    "idempotencyKey", UUID.randomUUID().toString(), "confirm", true));
            var failed = authenticatedPost(base + "/membership-changes/confirm", token, confirmation);
            assertThat(failed.statusCode()).isEqualTo(500);
            assertThat(failed.body()).contains("HTTP_500", "requestId").doesNotContain("synthetic audit failure", target);
            assertThat(owner.queryForObject("select count(*) from identity_membership where company_id=? and subject=?",
                    Integer.class, company, target)).isZero();
            assertThat(owner.queryForObject("select count(*) from identity_membership_change_audit where company_id=? and subject=?",
                    Integer.class, company, target)).isZero();
        } finally {
            owner.execute("DROP TRIGGER fail_synthetic_membership_audit ON identity_membership_change_audit");
            owner.execute("DROP FUNCTION public.fail_synthetic_membership_audit()");
        }
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
        int exit = com.olarize.identity.infrastructure.in.cli.ProvisionMembershipCommand.run(new String[]{"provision-membership"},
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

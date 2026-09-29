package com.olarize.integration;

import com.olarize.organization.application.port.in.CompanyLookupPort;
import com.olarize.organization.application.port.in.CompanyReadAuthorizationPort;
import com.olarize.organization.adapter.in.cli.ProvisionCompanyCommand;
import com.olarize.organization.adapter.out.persistence.CompanyLookupJdbcRepository;
import com.olarize.organization.adapter.out.persistence.CompanyRegistrationJdbcRepository;
import com.olarize.organization.domain.model.CompanyRegistration;
import com.olarize.organization.domain.exception.RegistrationConflict;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class CompanyProvisioningIntegrationTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
            .waitingFor(Wait.forListeningPort());
    static JdbcTemplate owner;
    static final String PASSWORD = "synthetic-only";

    static DriverManagerDataSource source(String username) {
        return new DriverManagerDataSource(POSTGRES.getJdbcUrl(), username, PASSWORD);
    }

    @BeforeAll static void initialize() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        owner = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        owner.execute("create role olarize_company_provisioner nologin nosuperuser nobypassrls");
        for (String user : new String[]{"operator_one", "operator_two", "web_runtime"}) {
            owner.execute("create role " + user + " login password 'synthetic-only' nosuperuser nobypassrls nocreatedb nocreaterole");
        }
        owner.execute("grant olarize_company_provisioner to operator_one, operator_two");
        owner.execute("grant usage on schema public to olarize_company_provisioner, web_runtime");
        owner.execute("grant select on organization_company, organization_company_creation to olarize_company_provisioner");
        owner.execute("grant execute on function public.provision_company(uuid, text, text, uuid, uuid) to olarize_company_provisioner");
        owner.execute("grant select on organization_company to web_runtime");
    }

    CompanyRegistrationJdbcRepository store(String user) { return new CompanyRegistrationJdbcRepository(source(user)); }
    CompanyRegistration request(UUID key) { return new CompanyRegistration("شركة Synthetic", "Synthetic setup", key); }

    @Test void commandCreatesAuditedIdentityAndRetryReturnsOriginal() throws Exception {
        UUID key = UUID.randomUUID();
        var output = new ByteArrayOutputStream();
        var error = new ByteArrayOutputStream();
        String input = "{\"name\":\"  شركة Synthetic  \",\"reason\":\"Synthetic setup\",\"idempotencyKey\":\"" + key + "\",\"confirm\":true}";
        var env = Map.of("ORG_PROVISION_DB_URL", POSTGRES.getJdbcUrl(), "ORG_PROVISION_DB_USERNAME", "operator_one", "ORG_PROVISION_DB_PASSWORD", PASSWORD);
        assertThat(ProvisionCompanyCommand.run(new String[]{"provision-company"}, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output), new PrintStream(error), env)).isZero();
        assertThat(error.toString()).isEmpty();
        var json = new JsonMapper().readTree(output.toString(StandardCharsets.UTF_8));
        UUID id = UUID.fromString(json.get("companyId").stringValue());
        assertThat(json.get("version").longValue()).isEqualTo(1);
        var company = store("operator_one").register(request(key), UUID.randomUUID());
        assertThat(company.id()).isEqualTo(id);
        assertThat(company.name()).isEqualTo("شركة Synthetic");
        assertThat(company.createdAt()).isEqualTo(company.updatedAt());
        var audit = owner.queryForMap("select * from organization_company_creation where company_id = ?", id);
        assertThat(audit).containsEntry("operator_name", "operator_one").containsEntry("action", "COMPANY_CREATED")
                .containsEntry("correlation_id", UUID.fromString(json.get("requestId").stringValue()));
        assertThat(owner.queryForObject("select count(*) from organization_company_creation where company_id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test void conflictDoesNotChangeCompanyAndKeysAreScopedToOperator() {
        UUID key = UUID.randomUUID();
        var original = store("operator_one").register(request(key), UUID.randomUUID());
        assertThatThrownBy(() -> store("operator_one").register(new CompanyRegistration("Changed", "Synthetic setup", key), UUID.randomUUID()))
                .isInstanceOf(RegistrationConflict.class);
        assertThatThrownBy(() -> store("operator_one").register(new CompanyRegistration("شركة Synthetic", "Changed reason", key), UUID.randomUUID()))
                .isInstanceOf(RegistrationConflict.class);
        assertThat(store("operator_one").register(request(key), UUID.randomUUID())).isEqualTo(original);
        assertThat(store("operator_two").register(request(key), UUID.randomUUID()).id()).isNotEqualTo(original.id());
    }

    @Test void concurrentRetriesCreateOneCompanyAndOneReceipt() throws Exception {
        UUID key = UUID.randomUUID();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<UUID> call = () -> {
                ready.countDown();
                start.await();
                return store("operator_one").register(request(key), UUID.randomUUID()).id();
            };
            var a = executor.submit(call);
            var b = executor.submit(call);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(a.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(b.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(owner.queryForObject("select count(*) from organization_company_creation where operator_name = 'operator_one' and idempotency_key = ?", Integer.class, key)).isEqualTo(1);
    }

    @Test void ordinaryAndOwnerCredentialsCannotUseCommandAndRuntimeCannotWriteDirectly() {
        assertThatThrownBy(() -> store("web_runtime").register(request(UUID.randomUUID()), UUID.randomUUID())).isInstanceOf(SecurityException.class);
        var privileged = new CompanyRegistrationJdbcRepository(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThatThrownBy(() -> privileged.register(request(UUID.randomUUID()), UUID.randomUUID())).isInstanceOf(SecurityException.class);
        var runtime = new JdbcTemplate(source("web_runtime"));
        assertThatThrownBy(() -> runtime.update("insert into organization_company(id,name,created_at,updated_at) values (?, 'Synthetic', now(), now())", UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> runtime.execute("set role olarize_company_provisioner")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(runtime.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
    }

    @Test void provisioningOperatorCannotBypassAtomicAuditWithDirectInserts() {
        UUID id = UUID.randomUUID();
        var operator = new JdbcTemplate(source("operator_one"));
        assertThatThrownBy(() -> operator.update("insert into organization_company(id,name,created_at,updated_at) values (?, 'Synthetic', now(), now())", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> operator.update("insert into organization_company_creation(company_id,operator_name,idempotency_key,reason,action,recorded_at,correlation_id) values (?,session_user,?,'reason','COMPANY_CREATED',now(),?)", id, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(owner.queryForObject("select count(*) from organization_company where id = ?", Integer.class, id)).isZero();
        assertThat(owner.queryForObject("select count(*) from organization_company_creation where company_id = ?", Integer.class, id)).isZero();
    }

    @Test void auditFailureRollsBackCompanyAndAllowsCleanRetry() {
        UUID key = UUID.randomUUID();
        int before = owner.queryForObject("select count(*) from organization_company", Integer.class);
        owner.execute("alter table organization_company_creation add constraint synthetic_audit_failure check (idempotency_key <> '" + key + "'::uuid)");
        try {
            assertThatThrownBy(() -> store("operator_one").register(request(key), UUID.randomUUID()))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Provisioning persistence failed");
            assertThat(owner.queryForObject("select count(*) from organization_company", Integer.class)).isEqualTo(before);
            assertThat(owner.queryForObject("select count(*) from organization_company_creation where idempotency_key = ?", Integer.class, key)).isZero();
        } finally { owner.execute("alter table organization_company_creation drop constraint synthetic_audit_failure"); }
        assertThat(store("operator_one").register(request(key), UUID.randomUUID()).id()).isNotNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(CompanyLookupJdbcRepository.class)
    static class LookupConfiguration {
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactions(DataSource source) { return new DataSourceTransactionManager(source); }
    }

    @Test void publicLookupUsesVerifiedScopeAndPooledTransactionsDoNotLeakContext() {
        var a = store("operator_one").register(request(UUID.randomUUID()), UUID.randomUUID());
        var b = store("operator_one").register(request(UUID.randomUUID()), UUID.randomUUID());
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl()); config.setUsername("web_runtime"); config.setPassword(PASSWORD); config.setMaximumPoolSize(1);
        try (var pool = new HikariDataSource(config); var context = new AnnotationConfigApplicationContext()) {
            AtomicReference<UUID> authorized = new AtomicReference<>(a.id());
            context.registerBean(DataSource.class, () -> pool);
            context.registerBean(CompanyReadAuthorizationPort.class, () -> id -> id.equals(authorized.get()));
            context.register(LookupConfiguration.class);
            context.refresh();
            CompanyLookupPort lookup = context.getBean(CompanyLookupPort.class);
            assertThat(lookup.find(a.id())).contains(a);
            assertThat(lookup.find(b.id())).isEmpty();
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            authorized.set(b.id());
            assertThat(lookup.find(b.id())).contains(b);
            assertThat(lookup.find(a.id())).isEmpty();
            var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            assertThatThrownBy(() -> transaction.execute(status -> {
                jdbc.queryForObject("select set_config('olarize.company_id', ?, true)", String.class, a.id().toString());
                throw new IllegalStateException("Synthetic rollback");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("select count(*) from organization_company", Integer.class)).isZero();
            authorized.set(null);
            assertThat(lookup.find(b.id())).isEmpty();
        }
    }

    @Test void lookupWithoutMembershipProviderFailsClosed() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class, () -> source("web_runtime"));
            context.register(LookupConfiguration.class);
            context.refresh();
            assertThat(context.getBean(CompanyLookupPort.class).find(UUID.randomUUID())).isEmpty();
        }
    }
}

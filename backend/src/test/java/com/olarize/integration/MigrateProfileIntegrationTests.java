package com.olarize.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles({"prod", "migrate"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MigrateProfileIntegrationTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void runtimeConfiguration(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.placeholders.db_scope_key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.add("olarize.identity.database-scope-key",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.add("olarize.auth.issuer", () -> "https://identity.example.test/realms/olarize");
        properties.add("olarize.auth.jwk-set-uri", () -> "https://identity.example.test/certs");
    }

    @Autowired JdbcTemplate database;

    @Test void migrationTaskAppliesSchemaWithoutStartingWebServer() {
        assertThat(database.queryForObject(
            "select count(*) from flyway_schema_history where version = '1' and success", Integer.class))
            .isEqualTo(1);
        assertThat(database.queryForObject("select extversion from pg_extension where extname = 'vector'", String.class))
            .isEqualTo("0.8.6");
        assertThat(database.queryForObject("select to_regclass('public.identity_membership') is not null", Boolean.class)).isTrue();
        assertThat(database.queryForObject("select to_regprocedure('public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid)') is not null", Boolean.class)).isTrue();
    }
}

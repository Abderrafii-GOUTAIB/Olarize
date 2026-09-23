# Olarize backend foundation

Date: 2026-09-22. Status: implementation in progress.

Use Java 21 LTS (already installed), Spring Boot 4.1.1, Spring Modulith 2.1.1, Spring AI BOM 2.0.1, Maven Wrapper 3.3.4 and Maven 3.9.16. Initializr recommends the Boot/Modulith combination; Spring AI documents Boot 4.0/4.1 support. Maven Central metadata confirms these are published releases. The Initializr response used a .RELEASE suffix for Boot; the POM uses the published Maven Central version 4.1.1.

Use PostgreSQL 17 with pgvector 0.8.6. Versioned image tags are pinned, not immutable digests; pin reviewed digests in the deployment pipeline. The Java package is com.olarize; existing scoped instruction paths were moved accordingly. Keep the existing physical project folder name unchanged.

Include Web MVC, Security, Validation, Actuator, JPA, Flyway, PostgreSQL, Modulith core and testing dependencies. Import Spring AI BOM only; provider starters arrive with the first AI slice, avoiding credentials/model requirements at startup. No empty business modules are generated.

Expose only sanitized GET health/liveness/readiness. Deny all other requests with no development user or login. CSRF remains enabled; Cognito authentication and domain authorization are the next task. Local Flyway runs on startup; shared environments must run controlled migrations before deploying runtime replicas. Local DB credentials use the development database owner; production needs separate migration/runtime roles.

References: https://start.spring.io ; https://docs.spring.io/spring-ai/reference/getting-started.html ; https://repo.maven.apache.org/maven2/ ; https://github.com/pgvector/pgvector

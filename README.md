# Olarize

AI-native HR for Moroccan private companies with approximately 20-200 employees. This repository contains the Spring Boot and Docker foundation plus backend slices for company setup, memberships and minimum employee creation. Keycloak authentication and GET /api/v1/identity are implemented. ORG-001 company provisioning is available through a restricted administrative command; see [company provisioning](docs/company-provisioning.md). EMP-001 provides membership listing and trusted company context; EMP-002 provides confirmed, company-scoped employee creation. Directory views, structured employee forms, other HR workflows, frontend and AI integrations remain planned.

## Stack

Java 21 LTS, Spring Boot 4.1.1, Spring Security, Web MVC, Validation, Actuator, JPA, Flyway and Spring Modulith 2.1.1. PostgreSQL 17 with pgvector 0.8.6 runs in Docker. Maven Wrapper pins Maven 3.9.16. Spring AI BOM 2.0.1 manages future AI versions; no model credentials/calls are required now.

## Folder organization

```text
olarize/                             Existing folder may still be named hr ai system
  AGENTS.md                          Small pointer to development guidance
  .agents/skills/                    Story, implementation and review skills
  .codex/agents/                     Project-local reviewer configuration
  .githooks/                         Versioned pre-commit checks
  .instructions/                     All agent rules, business references and tasks
  .github/workflows/backend.yml      Backend verification pipeline
  backend/
    pom.xml                          Dependencies and build configuration
    mvnw, mvnw.cmd, .mvn/             Official Maven Wrapper
    Dockerfile, .dockerignore         Non-root container and build context
    src/main/java/com/olarize/
      OlarizeApplication.java        Application entry point
      platform/
        package-info.java            Technical module boundary
        internal/security/           Security filter chain
        internal/web/                Error contract, request IDs, local API setup
      identity/
        package-info.java            Identity module boundary
        adapter/in/web/               HTTP controllers and API documentation
      employment/
        domain/                       Minimum employee facts and status
        application/                  Create use case, permission and transaction
        adapter/                      DTOs, JPA entities, repositories and mappers
    src/main/resources/
      application.yml                Safe common configuration
      application-local.yml          Explicit local migration behavior
      application-dev.yml            Shared development runtime
      application-prod.yml           Production runtime
      application-migrate.yml        One-off migration task
      db/migration/                  Versioned Flyway migrations
    src/test/java/com/olarize/        Architecture checks
      integration/                   PostgreSQL and HTTP integration tests
      platform/internal/web/         HTTP errors and correlation tests
  deploy/                            Host-neutral dev/prod release contract
  scripts/                           Run and verification helpers
  docs/                              Human-facing architecture and runbooks
  compose.yaml                       Local database and optional app
  compose.debug.yaml                 Opt-in local debugger
  .env.example                       Non-secret local configuration template
```

infra/keycloak/ contains the local realm import. The frontend implementation folder will be added when its work begins. Agent instructions do not live inside application source packages.

## Clean code and clean architecture

Use package-by-business-module, then separate public contracts, domain rules, application ports/services and adapters as a feature requires them. Name application service classes with the `Service` suffix. `com.olarize` is the Java package prefix and contains only the application entry point; it is not a catch-all feature package. Dependency direction points toward the domain. Domain code must not import Spring, JPA, HTTP or AI providers. Use small cohesive classes, constructor injection, explicit types and meaningful business names. Avoid global utility/service classes and speculative abstractions.

Business modules follow `domain/{model,service,event,exception}`, `application/{port/in,port/out,service,command,query}`, and `adapter/{in/{web,messaging,cli},out/{persistence,messaging,external}}` when those folders are needed. We do not create empty folders. The current `identity` and `organization` source trees are examples.

Controllers keep only HTTP mapping and input/output mapping annotations. Endpoint documentation lives beside the web adapter in local OpenAPI configuration; authentication rules remain in the security configuration. Do not add an interface or empty application/domain layer solely to hide annotations.

Modules communicate through public APIs or durable events, never another module's repository. Controllers and AI tools reuse the same authorization and deterministic business services. Flyway owns schema changes; JPA validates. Domain tests verify rules, integration tests verify real PostgreSQL behavior, and Modulith/ArchUnit checks protect boundaries. Empty future business packages are intentionally absent.

See [architecture](docs/architecture.md) for the module template and dependency diagram. See [errors and debugging](docs/errors-and-debugging.md) for request IDs, problem responses and debugger configuration.

For shared environments, use the [deployment contract](deploy/README.md). It defines separate dev/prod runtime profiles and a one-off migration profile; the local Compose stack is only for workstation development. A cloud or VPS deployment pipeline will be added after the hosting target is chosen.

## Run

On first setup only, copy .env.example to .env and set a local-only password. Keep an existing .env unchanged. Java 21 and a running Docker engine are required for host development.

```sh
docker compose --profile app up -d --build --wait
curl --fail http://localhost:8080/actuator/health
```

For Java running on your host:

```sh
docker compose up -d db keycloak --wait
./scripts/run-backend.sh
```

Default ports: app localhost:8080, database localhost:5433. Edit .env if those ports are occupied. This is a local development setup, not production infrastructure.

For IntelliJ, select the `local` profile and use the repository root or `backend/` as the working directory. The application reads credentials from the root `.env`; do not copy them into the IDE. See [development](docs/development.md) for the supported file format.

## Test and debug

```sh
./scripts/verify-backend.sh
./scripts/run-backend.sh --debug
```

Attach a JVM debugger to localhost:5005 for the second command. For container debugging, use the override documented in [debugging](docs/errors-and-debugging.md). Debugging is off by default.

Tests use a separate disposable PostgreSQL/pgvector container. Do not bypass failing tests. GitHub Actions runs verification and builds the Docker image; cloud deployment is not configured.

## Security and current limits

Health, liveness and readiness are public. The local profile also serves Swagger UI and generated OpenAPI. GET /api/v1/identity requires a validated Keycloak access token and profile.read scope; other HR routes remain denied. Errors use sanitized problem responses and request correlation. Credentials remain in ignored .env, not source. Docker runs the backend as a non-root user and exposes local ports on loopback.

Local Flyway enables the vector extension. Shared environments must run controlled migrations with separate privileges; startup migrations are disabled by default. A vector extension is not a working RAG pipeline. No frontend, payroll logic, CNSS/CIMR calculations or official integrations are claimed complete.

See the visual [feature map and current flows](docs/features-and-flows.md) for what works today and how to check it. Read [development](docs/development.md) for commands, troubleshooting and actual verification status. The next implementation contract is [.instructions/tasks/002-first-ai-workflow.md](.instructions/tasks/002-first-ai-workflow.md).

## Authentication

See [Keycloak setup and endpoint guide](docs/authentication.md) and the [API contract](docs/api/openapi.yaml). Keycloak runs locally at http://localhost:8081 with a separate PostgreSQL database. Local credentials are in .env. No AWS account is needed for authentication.

## Swagger UI and logs

With the local profile, open [Swagger UI](http://localhost:8080/swagger-ui/index.html) or fetch `http://localhost:8080/v3/api-docs`. The Authorize button signs into the local Keycloak realm using PKCE; create a local user first as described in the authentication guide. Swagger is disabled outside the local profile. Local IDE runs bind to loopback; Docker publishes the backend on 127.0.0.1.

Docker writes structured JSON logs to standard output; `docker compose logs --tail=100 backend` shows them. Each non-health request logs method, status, duration and request ID. Paths, queries, headers, bodies and tokens are not logged by the request filter. See [logging and debugging](docs/errors-and-debugging.md).

## Interactive development studio

The standalone [developer workbench](developer-workbench/README.md) lives outside the product frontend. It displays module discovery, story selection, implementation, verification, independent review and PR handoffs. Open `developer-workbench/index.html` or run:

```sh
python3 -m http.server 4178 --bind 127.0.0.1 --directory developer-workbench
```

Visit http://127.0.0.1:4178. It contains a local snapshot of employee stories and browser-local manual progress; it does not execute agents or synchronize Jira/GitHub.

The connected development service is now available via `./developer-workbench/start.sh`. Open its private launch URL for live run controls, diff review and publication gates. Read [the connected workbench guide](developer-workbench/README.md) before enabling existing Codex login access. Static `index.html` remains the manual planner.

## MVP customer-data direction

The accepted target is managed hosting in Morocco, with a provider still to be chosen. Company isolation, restricted operations, private documents and verified backup recovery must be implemented and tested before real customer onboarding. The current MVP excludes external AI processing of employee/confidential company data; development agents and tests use synthetic data only. See [ADR 003](docs/decisions/003-mvp-data-protection-and-hosting.md) and the [security contract](.instructions/reference/security.md). This records the direction, not a claim that these protections are already deployed.

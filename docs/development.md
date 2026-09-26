# Olarize local backend

Scope: backend and Docker foundation only. Keycloak and GET /api/v1/me are implemented; frontend, company memberships, HR endpoints and AI integrations are not implemented. Java 21 and Docker Compose are required; Maven downloads through the committed official wrapper. No AWS credentials are required.

## Start with Docker

From the project root:

```sh
# First setup only: preserve an existing .env.
cp .env.example .env
# Edit .env and set a local-only POSTGRES_PASSWORD.
docker compose --profile app up -d --build --wait
curl --fail http://localhost:8080/actuator/health
```

The result includes status UP; it may also list the liveness/readiness groups. The API binds to localhost:8080 and PostgreSQL to localhost:5433 by default. Override BACKEND_PORT and POSTGRES_PORT in .env if occupied. `.env` is ignored; never use production secrets here. The local profile imports this file as Java properties; the helper script and Docker Compose also read it. Use plain KEY=value entries and unquoted alphanumeric local passwords (no export prefixes, shell expansion, backslashes or surrounding quotes) to keep all readers consistent.

## Run Java on the host

```sh
docker compose up -d db keycloak --wait
./scripts/run-backend.sh
```

## Verify

```sh
./scripts/verify-backend.sh
```

Tests start a separate disposable PostgreSQL/pgvector container. They verify Flyway, vector operations, health, closed routes and the Modulith boundary check. A running Docker engine is required and tests must fail rather than silently skip if unavailable. On Rancher Desktop, use the Docker context socket if Testcontainers does not detect it: set DOCKER_HOST to the endpoint from `docker context inspect --format '{{.Endpoints.docker.Host}}'`. Container socket mounts may need TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock; do not disable cleanup.

Tests under `backend/src/test/java/com/olarize/integration/` start disposable PostgreSQL and exercise the running HTTP server or migration context. The dev/prod profile tests verify readiness, disabled Swagger and no runtime migrations. The migration test verifies Flyway and pgvector. The signed-token HTTP integration tests cover `/api/v1/me` and authorization failures. Focused tests can be run with `./scripts/verify-backend.sh -Dtest=ProdProfileIntegrationTests`; the full command above remains the CI gate.

## Stop and inspect

```sh
docker compose --profile app logs --tail=100 backend
docker compose --profile app down
```

Stopping preserves the named database volume. Do not add `-v` unless intentionally deleting local data. Changing .env credentials does not change users in an existing database volume. Inspect your existing state rather than deleting data to resolve a mismatch.

## Configuration and boundaries

DB_URL, DB_USERNAME and DB_PASSWORD are required outside local orchestration. No implicit local profile is enabled. Only local runs enable automatic Flyway in application replicas. Shared dev/prod runtimes disable Flyway; a separate `dev,migrate` or `prod,migrate` task runs migrations once with a dedicated role before rollout. JPA validates schema, never creates it. Readiness checks database access; health output hides infrastructure details. See [deployment contract](../deploy/README.md).

Public endpoints: GET /actuator/health, /actuator/health/liveness and /actuator/health/readiness. Local runs also expose Swagger UI and generated OpenAPI. GET /api/v1/me requires a valid scoped Keycloak token; other HR access remains denied. Spring AI versions are managed but no model starter or vector-store auto-initialization is active. Database extension readiness is not implemented RAG.

See docs/decisions/001-olarize-foundation.md for dependencies. Verified on 2026-09-23: 26 backend tests passed, Docker Compose was healthy, and the Keycloak PKCE login and identity endpoint passed a live local smoke check. GitHub Actions has not been run remotely. Task 001 under .instructions/tasks/ remains partial until its separate frontend scope is implemented.

Backend CI is defined in .github/workflows/backend.yml with immutable action revisions. It runs tests, validates Compose and builds the image; it does not deploy.

Use `./scripts/run-backend.sh --debug` for a loopback-only host debugger. Detailed error/debugging behavior is documented in docs/errors-and-debugging.md.

## IntelliJ with the shared .env

Select OlarizeApplication, set Active profiles to local (or program arguments --spring.profiles.active=local), and use the repository root or backend directory as the working directory. Remove manually copied DB_URL, DB_USERNAME and DB_PASSWORD variables if present: explicitly supplied environment variables override file settings. Start the Compose database, then run the application. The local profile imports the root .env and maps POSTGRES_* values to the datasource; no credentials belong in the IDE configuration. Other profiles do not import .env. Docker still supplies DB_URL pointing to its db service.

The import uses Spring Boot's native properties format, not a full dotenv parser. Keep the .env format described above. See https://docs.spring.io/spring-boot/reference/features/external-config.html for configuration imports.

See authentication.md for local Keycloak, required additional .env passwords and the first endpoint.

## Swagger and logs

Swagger UI is enabled in the local profile at http://localhost:8080/swagger-ui/index.html; generated OpenAPI is at http://localhost:8080/v3/api-docs. The Authorize action uses Keycloak authorization code + PKCE and requests profile.read. Create a local realm user first; the Keycloak bootstrap administrator is not an Olarize user. The Swagger redirect is an exact allowed URI for the local client. Nonlocal profiles disable both routes and security denies access. The checked-in docs/api/openapi.yaml is a reference contract; the generated OpenAPI reflects the running controllers.

Docker console logs use Spring Boot's ECS JSON format. Inspect them with `docker compose logs --tail=100 backend`; the requestId field matches the X-Request-ID response header. Host IDE runs retain readable text logs. Request completion logs contain method, status and elapsed milliseconds, skip health polling, and omit request path, query, headers, body and token. Unexpected errors log an exception class and request ID. DEBUG stack traces are for local synthetic data only. Docker collects standard output locally; a shared deployment still needs a log collector, retention controls, alerts and restricted access.

Verified on 2026-09-23: 37 backend tests passed from a clean build, including PostgreSQL/HTTP integration tests for authentication, local Swagger visibility, shared dev/prod profile isolation, mixed-profile safety, migration mode, and safe request logging. The backend Docker image built successfully and `docker compose config --quiet` passed. Live Docker checks from the earlier local setup confirmed Swagger HTML and assets, generated OAuth metadata, PKCE client settings, Keycloak browser CORS, and ECS JSON request logs with matching request IDs. No shared dev or production environment has been deployed.

## Development agents and Git hooks

Follow [the development workflow](../.instructions/development-workflow.md) for one-story implementation, independent review and fixes. Enable the project-local pre-commit hook once per clone:

```sh
./scripts/install-hooks.sh
```

The hook checks staged whitespace and conflict markers without changing files. Full tests remain in `./scripts/verify-backend.sh` and CI. The reviewer configuration is `.codex/agents/olarize-reviewer.toml`; it reviews using the existing `olarize-review` skill and returns findings to the developer. A fresh Codex task opened in this repository may be needed to discover the role.

## Connected developer workbench

Run `./developer-workbench/start.sh` and open its private local launch URL. Read [the workbench guide](../developer-workbench/README.md) for live execution prerequisites, approval gates and current limits. The original static planner remains separate. Verify this tool with `PYTHONDONTWRITEBYTECODE=1 python3 developer-workbench/service/test_workbench.py` and `node --check developer-workbench/connected.js`; these checks do not run Olarize HR features or publish external issues/PRs.

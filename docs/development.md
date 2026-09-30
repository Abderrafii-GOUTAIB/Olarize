# Olarize local backend

Scope: backend and Docker foundation with ORG-001 company provisioning, EMP-001 active membership/company-context APIs, IAM-001 company-scoped membership administration, IAM-002 PostgreSQL isolation, and EMP-002 confirmed minimum employee creation. Keycloak and GET /api/v1/identity are implemented. Employee directory views, structured forms, other HR features, frontend and AI integrations are not implemented yet. Java 21 and Docker Compose are required; Maven downloads through the committed official wrapper. No AWS credentials are required.

For a visual map of implemented features, current request flows and manual verification steps, see [features and flows](features-and-flows.md).

## Start with Docker

From the project root:

```sh
# First setup only: preserve an existing .env.
cp .env.example .env
# Edit .env and set a local-only POSTGRES_PASSWORD.
docker compose --profile app up -d --build --wait
curl --fail http://localhost:8080/actuator/health
```

The result includes status UP; it may also list the liveness/readiness groups. The API binds to localhost:8080 and PostgreSQL to localhost:5433 by default. Override BACKEND_PORT and POSTGRES_PORT in .env if occupied. Compose passes BACKEND_PORT into Keycloak so the imported Swagger callback uses the backend's configured port; KEYCLOAK_PORT controls Keycloak's separate host port. `.env` is ignored; never use production secrets here. The local profile imports this file as Java properties; the helper script and Docker Compose also read it. Use plain KEY=value entries and unquoted alphanumeric local passwords (no export prefixes, shell expansion, backslashes or surrounding quotes) to keep all readers consistent. Local PostgreSQL has a separate `olarize_runtime` role for the API; Flyway uses the bootstrap DB account to create that role and grant only local app permissions. Keep `LOCAL_RUNTIME_PASSWORD` different from `POSTGRES_PASSWORD`. Set `DB_SCOPE_KEY` to a separate 64-character hex value (generate with `openssl rand -hex 32`) before applying V6; existing `.env` files need this new line. The backend and Flyway migration must receive the same value.

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

Tests under `backend/src/test/java/com/olarize/integration/` start disposable PostgreSQL and exercise the running HTTP server or migration context. The dev/prod profile tests verify readiness, disabled Swagger and no runtime migrations. The local Swagger test verifies the persistent synthetic demo company and membership rows are seeded by the local-only Flyway location. The migration test verifies Flyway and pgvector. The signed-token HTTP integration tests cover `/api/v1/identity`, active membership listing, explicit company selection, revocation and authorization failures. IAM-002 adds restricted-runtime PostgreSQL checks for company reads, joins, revocation, transaction cleanup, audit references and role privileges. Focused tests can be run with `./scripts/verify-backend.sh -Dtest=ProdProfileIntegrationTests`; the full command above remains the CI gate. See [company isolation](company-isolation.md) for the current policy and requirements for future tables.

Existing local databases already contain demo migrations V900/V901. The local profile allows out-of-order Flyway execution so a new shared migration such as V6 can be applied later without deleting the persistent demo data. Shared dev/prod migration jobs keep normal ordering.

## Stop and inspect

```sh
docker compose --profile app logs --tail=100 backend
docker compose --profile app down
```

Stopping preserves the named database volume. Do not add `-v` unless intentionally deleting local data. Changing .env credentials does not change users in an existing database volume. Inspect your existing state rather than deleting data to resolve a mismatch.

## Configuration and boundaries

DB_URL, DB_USERNAME, DB_PASSWORD and DB_SCOPE_KEY are required outside local orchestration. Supply the same scope key to the one-off migration task and all backend replicas through deployment secrets; it must be separate from the runtime database password. No implicit local profile is enabled. Only local runs enable automatic Flyway in application replicas. Shared dev/prod runtimes disable Flyway; a separate `dev,migrate` or `prod,migrate` task runs migrations once with a dedicated role before rollout. JPA validates schema, never creates it. Readiness checks database access; health output hides infrastructure details. See [deployment contract](../deploy/README.md) and [company isolation](company-isolation.md).

Public endpoints: GET /actuator/health, /actuator/health/liveness and /actuator/health/readiness. Local runs also expose Swagger UI and generated OpenAPI. GET /api/v1/identity and the EMP-001 membership/company-context endpoints require a valid Keycloak token with profile.read. IAM-001 adds scoped membership list, exact-change preview and confirmation endpoints; these also require current database `identity.admin` rights for the selected company. See [membership administration](membership-administration.md) for restricted database-role and Keycloak service-account setup. Other HR routes remain denied. Spring AI versions are managed but no model starter or vector-store auto-initialization is active. Database extension readiness is not implemented RAG.

See docs/decisions/001-olarize-foundation.md for dependencies. Verified on 2026-09-23: 26 backend tests passed, Docker Compose was healthy, and the Keycloak PKCE login and identity endpoint passed a live local smoke check. GitHub Actions has not been run remotely. Task 001 under .instructions/tasks/ remains partial until its separate frontend scope is implemented.

Backend CI is defined in .github/workflows/backend.yml with immutable action revisions. It runs tests, validates Compose and builds the image; it does not deploy.

Use `./scripts/run-backend.sh --debug` for a loopback-only host debugger. Detailed error/debugging behavior is documented in docs/errors-and-debugging.md.

## IntelliJ with the shared .env

Select OlarizeApplication, set Active profiles to local (or program arguments --spring.profiles.active=local), and use the repository root or backend directory as the working directory. Remove manually copied DB_URL, DB_USERNAME and DB_PASSWORD variables if present: explicitly supplied environment variables override file settings. Start the Compose database, then run the application. The local profile imports the root .env and maps POSTGRES_* values to the datasource; no credentials belong in the IDE configuration. Other profiles do not import .env. Docker still supplies DB_URL pointing to its db service.

The import uses Spring Boot's native properties format, not a full dotenv parser. Keep the .env format described above. See https://docs.spring.io/spring-boot/reference/features/external-config.html for configuration imports.

See authentication.md for local Keycloak, required additional .env passwords and the first endpoint.

## Swagger and logs

EMP-002 adds a confirmed HR employee-creation POST in local Swagger. The local `V902` migration persists a synthetic employee, and `V903` gives `olarize-demo-admin` an explicit `employee.write` permission for Swagger checks. Set the synthetic user's password as described in [employee creation](employment-creation.md). That page gives the exact company ID, request and expected response. The directory read endpoint and accessible form remain separate stories.

Swagger UI is enabled in the local profile at http://localhost:8080/swagger-ui/index.html; generated OpenAPI is at http://localhost:8080/v3/api-docs. The Authorize action uses Keycloak authorization code + PKCE and requests profile.read. The local profile applies `db/local` Flyway seeds once; demo company and membership rows persist in the PostgreSQL volume. The web datasource connects as restricted `olarize_runtime`; Flyway uses the local bootstrap account separately. To see nonempty membership responses, set up the synthetic `olarize-demo-admin` account using [local authentication](authentication.md), then call `GET /api/v1/identity/memberships`, `GET /api/v1/identity/company-context`, and `GET /api/v1/companies/{companyId}/memberships` using the returned company ID. The Keycloak bootstrap administrator is not an Olarize user. The Swagger redirect is an exact allowed URI for the local client. Nonlocal profiles disable both routes and security denies access. The checked-in docs/api/openapi.yaml is a reference contract; the generated OpenAPI reflects the running controllers.

Docker console logs use Spring Boot's ECS JSON format. Inspect them with `docker compose logs --tail=100 backend`; the requestId field matches the X-Request-ID response header. Host IDE runs retain readable text logs. Request completion logs contain method, status and elapsed milliseconds, skip health polling, and omit request path, query, headers, body and token. Unexpected errors log an exception class and request ID. DEBUG stack traces are for local synthetic data only. Docker collects standard output locally; a shared deployment still needs a log collector, retention controls, alerts and restricted access.

Verified on 2026-09-23: 37 backend tests passed from a clean build, including PostgreSQL/HTTP integration tests for authentication, local Swagger visibility, shared dev/prod profile isolation, mixed-profile safety, migration mode, and safe request logging. The backend Docker image built successfully and `docker compose config --quiet` passed. Live Docker checks from the earlier local setup confirmed Swagger HTML and assets, generated OAuth metadata, PKCE client settings, Keycloak browser CORS, and ECS JSON request logs with matching request IDs. No shared dev or production environment has been deployed.

## Development agents and Git hooks

Follow [the development workflow](../.instructions/development-workflow.md) for one-story implementation, independent review and fixes. Enable the project-local Git hooks once per clone:

```sh
./scripts/install-hooks.sh
```

The pre-commit hook requires an action-based branch (`feature/OLARIZE-18` for a feature or `fix/OLARIZE-42` for a fix) and checks staged whitespace and conflict markers. The commit-msg hook requires a subject such as `OLARIZE-18: add company provisioning`, with the ticket taken from the active branch and a nonempty description. Both hooks block invalid commits and stay silent on success. They do not change files. Run `python3 scripts/test-git-hooks.py` to verify the hooks in disposable local repositories. Full tests remain in `./scripts/verify-backend.sh` and CI. The reviewer configuration is `.codex/agents/olarize-reviewer.toml`; it reviews using the existing `olarize-review` skill and returns findings to the developer. A fresh Codex task opened in this repository may be needed to discover the role.

## Connected developer workbench

Run `./developer-workbench/start.sh` and open its private local launch URL. Read [the workbench guide](../developer-workbench/README.md) for live execution prerequisites, approval gates and current limits. The original static planner remains separate. Verify this tool with `PYTHONDONTWRITEBYTECODE=1 python3 developer-workbench/service/test_workbench.py` and `node --check developer-workbench/connected.js`; these checks do not run Olarize HR features or publish external issues/PRs.

## Company provisioning

ORG-001 adds a separate `provision-company` command to the packaged backend. It uses personal restricted database credentials, stdin JSON and atomic company/audit persistence. See [the provisioning runbook](company-provisioning.md) for role grants, inputs, retries and exit codes. It does not start the web server. EMP-001 now provides the membership and company-context API.

Verified on 2026-09-28 for ORG-001: all 51 unit/architecture/HTTP/PostgreSQL tests passed with no skips, including eight company provisioning integration tests; the backend jar was packaged. The command smoke check returned sanitized invalid-input JSON without starting Spring or HTTP. The `provision-membership` command likewise uses a restricted, separate database role and never exposes a public bootstrap endpoint. See [membership provisioning](identity-provisioning.md) for its operator procedure. Independent review findings were fixed and re-reviewed. See [task 004](../.instructions/tasks/004-company-provisioning.md) for evidence and remaining production prerequisites.

## Automatic pull requests after commits

After `./scripts/install-hooks.sh`, a successful commit on `feature/OLARIZE-<number>` or `fix/OLARIZE-<number>` runs the post-commit hook. It pushes that branch to `origin` and creates a PR into `develop`, or pushes updates to its existing open PR. This workflow is authorized by the user's project preference; no workbench is required. It never merges or force-pushes.

The PR title is `<ticket>: <Jira summary>` and its description links the Jira issue. Before implementation, `olarize-feature` reads the live Jira ticket and saves its exact key, summary and URL in `.github/jira/<ticket>.json`. This file contains no credentials. Refresh it from Jira if the ticket title changes before publication.

Git uses its configured push authentication. GitHub API access uses `GITHUB_TOKEN` or `GH_TOKEN`, or the configured HTTPS Git credential helper. Keep credentials out of source. Failed publication leaves the local commit intact and prints a retry command: `python3 scripts/publish-pr.py`. The pre-commit and commit-msg checks still run before the commit; backend verification and CI remain separate.

Verify publication behavior without network calls using `python3 scripts/test-publish-pr.py`.

Verified 2026-09-28 for EMP-001: after the package/service naming refactor and company-lookup pool-contention fix, the full backend suite passed 63 tests with zero failures/errors/skips, including PostgreSQL/HTTP tests for concurrent company lookups with a two-connection pool. The packaged command smoke check returned sanitized invalid-input JSON without starting Spring or HTTP. Independent review found and verified the pool-contention fix; no open P0–P2 findings remain. See [task 005](../.instructions/tasks/005-company-context.md) for evidence and remaining OD-03 prerequisites.

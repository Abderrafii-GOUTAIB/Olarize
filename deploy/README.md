# Deployment contract (host-neutral)

This directory defines how to run the tested backend image in a shared development or production environment. It does not provision infrastructure or deploy anything. Choose a hosting target later; keep the application image and database migration procedure the same.

| Concern | Shared dev | Production |
|---|---|---|
| Runtime profile | `dev` | `prod` |
| Migration task profile | `dev,migrate` | `prod,migrate` |
| Database | Dedicated PostgreSQL 17 with reviewed pgvector support | Separate PostgreSQL 17 with backups and restore testing |
| Identity | Dedicated HTTPS Keycloak realm/issuer and database | Separate HTTPS Keycloak realm/issuer and database |
| Swagger UI | Disabled | Disabled |
| Flyway in runtime replicas | Disabled | Disabled |
| Logs | ECS JSON on stdout | ECS JSON on stdout with access control and retention |

The local `compose.yaml` and `local` profile are for a developer workstation. Never use Keycloak `start-dev`, `.env`, the debug override or the local profile in a shared environment. Host development keeps its current `.env` workflow. If `local` is accidentally combined with `dev` or `prod`, the local configuration and Swagger beans stay inactive; deploy with only the intended profile regardless.

## Image and runtime inputs

Build and test once. Tag the resulting image immutably (prefer its digest), then promote that same image from dev to production. The runtime needs these values from environment configuration or a secret manager:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` for application replicas |
| `DB_URL` | JDBC URL for that environment's private Olarize database |
| `DB_USERNAME`, `DB_PASSWORD` | Least-privilege runtime database credentials |
| `AUTH_ISSUER` | Exact public HTTPS Keycloak realm issuer for that environment |
| `AUTH_JWK_SET_URI` | Reachable HTTPS JWKS endpoint for that issuer |
| `AUTH_AUDIENCE` | Expected API audience; default `olarize-api` |
| `AUTH_CLIENT_ID` | Authorized SPA client; default `olarize-web` |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | Optional override; dev/prod already select `ecs` |

Create a separate Keycloak database and Olarize database per environment. Configure exact frontend callback and logout URLs in each Keycloak client. Keep secrets out of images, Git and logs. Supply TLS at the ingress and allow database access only from the intended application and migration tasks.

## Release order

1. Run `./scripts/verify-backend.sh` and build the image in CI. Ordinary pull requests use disposable PostgreSQL and synthetic tokens; no shared credentials are required.
2. Back up the destination database and review the Flyway SQL for compatibility. Use separate migration credentials with the privileges required by that migration. Migration V1 creates the pgvector extension, so the database operator must approve extension privileges before first deployment.
3. Run **one** migration task using the same image and `SPRING_PROFILES_ACTIVE=dev,migrate` or `prod,migrate`, with the migration database credentials. The `migrate` profile starts no HTTP server, runs Flyway, validates JPA mapping and exits. Do not run this profile on normal replicas. Wait for exit code 0 before rollout.
4. Start or update runtime replicas with only `dev` or `prod`, using restricted runtime credentials. Do not set `FLYWAY_ENABLED=true` on replicas. Verify `/actuator/health/readiness` and the expected application version, then promote traffic.
5. If rollout fails, restore the previous image only when its schema remains compatible. Do not assume that rolling back Java reverses a database migration; use an approved recovery plan or backup restore.

No provider-specific deployment workflow is configured yet. Once a host is chosen, put its infrastructure and environment manifests under `deploy/<provider>/dev` and `deploy/<provider>/prod`, and add a protected deployment pipeline that promotes the tested image. Avoid separate code branches or builds for dev and production.

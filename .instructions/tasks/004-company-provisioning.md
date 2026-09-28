# Task 004 — ORG-001 company provisioning

Status: implemented and verified locally on 2026-09-28; not deployed. Approved contract: docs/requirements/organization-identity.md, ORG-001 / OLARIZE-18; Jira checked against local requirements on 2026-09-28.

## User outcome

A controlled operator creates a company once using a non-HTTP command, receiving its immutable ID/version. Authentication is the dedicated PostgreSQL session login, not a caller-supplied actor.

## Scope and dependencies

Existing backend/Flyway only. Organization owns company identity, creation audit and retry evidence. No membership, frontend or AI implementation. No public company route. Lookup uses a narrow authorization SPI, absent until EMP-001 and denied by default.

## Contract

`java -jar backend/target/olarize-backend.jar provision-company` consumes bounded JSON on stdin: name, reason, idempotencyKey (UUID), confirm (true). Unknown/duplicate fields, trailing JSON and invalid data fail without writes. Credentials use separate ORG_PROVISION_DB_* environment variables. Dedicated personal database login is a member of a NOLOGIN provisioning group, never a superuser, owner or runtime credential. Company, audit and idempotency evidence commit together through `public.provision_company(...)`, a fixed-search-path database function. Operator roles have SELECT and function EXECUTE privileges, with no direct INSERT/UPDATE/DELETE. Same operator/key/normalized payload returns original result; changed payload conflicts. New correlation UUID per invocation; original creation correlation retained in audit. Operational setup grants only required database privileges; migrations do not create login credentials.

## Acceptance checks

Success, normalized retry, conflicting retry, concurrent retries, independent operator keys, Unicode/limits, explicit confirmation, unknown/duplicate input rejection, ordinary credentials denied, HTTP denied, scoped lookup cannot reveal B under A, audit failure rolls back creation, and provisioning operators cannot insert company or audit rows directly. Actual PostgreSQL roles and policies are exercised. Company lookup remains denied without trusted membership integration.

## Verification

- `./scripts/verify-backend.sh`: all 51 tests passed, zero failures/errors/skips, including eight real PostgreSQL company provisioning integration tests. Earlier attempts were blocked by the unavailable Docker runtime; final run used Rancher Desktop after it became available.
- Fresh rerun on 2026-09-28: `./scripts/verify-backend.sh` completed with BUILD SUCCESS (exit 0), 51 tests and a rebuilt executable jar. The initial sandbox run could not access Docker; the approved rerun outside the sandbox passed.
- Packaged command smoke check: `java -jar backend/target/olarize-backend.jar provision-company` with `{}` returned exit 2 and sanitized JSON stderr, no stdout or Spring/web startup.
- Independent `olarize-reviewer` inspected the implementation against base `dc674e7c5fe5356526587b59327ab9b349650a76`. Two P2 test findings were fixed: isolate nested configuration with `@TestConfiguration`; give invalid-input tests full synthetic configuration and a valid-input control so missing settings cannot mask regressions. Focused re-review confirmed both resolved and no new findings. Its re-review preceded the final full-suite result; the final 51-test evidence is recorded above.
- `git diff --check` passed.

## Completion notes

Organization owns validated company creation and immutable audit/retry receipts. JDBC is used for explicit transaction locking, database-session identity and row policies; a focused persistence port keeps command orchestration separate from SQL. The module API offers an authorized company lookup; no authorization provider is supplied before EMP-001, so normal application access remains denied. V2 creates the schema with FORCE RLS and no PUBLIC grants; separate operational grants and named operator logins are documented in `docs/company-provisioning.md`.

No frontend, membership management, external AI or new HTTP endpoints were added. Named production provisioning authority remains OD-03. No production data, deployment, commit, push or Jira transition is part of this implementation.

# Task 005 — EMP-001 trusted company context

Status: implemented and verified locally; not committed or deployed. Approved contract: docs/requirements/employee.md EMP-001 / OLARIZE-1, with the approved prerequisite refinement in organization-identity.md. Live Jira compared on 2026-09-28. Base: origin/develop b3cacf6; ORG-001 merged.

## User outcome

An authenticated user lists active memberships and resolves an authorized company for each request. Database revocation takes effect on the next request even with the same access token.

## Scope and dependencies

Identity owns memberships, explicit permissions, trusted context and controlled initial identity-administrator provisioning/revocation. Organization owns company identity. No HR records, public membership writes, frontend, role-management UI or AI.

## Contract

GET /api/v1/identity/memberships returns active memberships only. GET /api/v1/identity/company-context?companyId=<uuid> validates the requested company. Omitting company selects the sole active membership; multiple memberships require explicit selection (400), none denies scope (403). Both routes require profile.read and derive subject from the validated access token. Caller-supplied subject parameters are rejected. Malformed company IDs return 400; forbidden scopes return sanitized 403.

A public identity API exposes the same resolver without accepting caller-supplied subjects. Subject context is transaction-local for RLS; pooled connections cannot retain it. Organization lookup authorization uses current memberships. The restricted non-HTTP provision-membership command grants/revokes only identity.admin. Personal operator DB login is the actor; verified Keycloak-subject evidence, reason, explicit confirmation, expected version and idempotency key are required. Membership and immutable receipt commit together. Runtime credentials cannot provision. Normal role administration is IAM-001.

## Acceptance checks

Active A/B and revoked C; explicit B; ambiguous selection; malformed ID; empty memberships; forged subject; foreign company; unchanged token after revocation; permission isolation; missing subject and pooled reuse; ordinary/owner credentials denied; confirmed command success/retry/conflict/concurrency, stale version and audit rollback.

## Verification

Focused run: 34 tests across membership rules, PostgreSQL/HTTP, migrations, Foundation and architecture; all passed after fixing recursive company-name authorization lookup. Initial full suite: 62 tests passed with zero failures/errors/skips. Per user direction, identity and organization now follow the module layout: domain models/exceptions; application inbound/outbound ports and suffixed services; input web/CLI/security adapters; output persistence adapters. Their prior `api` and `internal` folders are removed. Only populated folders are kept; CLI and security are real input adapters beyond the web/messaging template. The root dispatcher has explicit Modulith dependencies on each module's named CLI entry point. The architecture test and project guidance enforce the boundaries. Generated OpenAPI exposed query-map documentation drift; the controller now declares `companyId` explicitly as a UUID and continues rejecting unknown/duplicate parameters. The packaged command smoke check returned sanitized invalid-input JSON without web startup. Independent review found that company lookup held a connection while checking membership; the authorization check now runs before the company-read transaction, and a synchronized eight-request PostgreSQL/HTTP regression test uses a two-connection pool. Focused re-review confirmed the fix. Final `./scripts/verify-backend.sh` passed 63 tests with zero failures/errors/skips and packaged the executable JAR.

## Completion notes

Production operator authority and live account verification remain OD-03. Synthetic fixtures only. Operator evidence records a prior verification against the configured Keycloak realm; it is not a live identity-provider lookup. No public self-enrollment. API requires profile.read; production operator authority and live account verification are still prerequisites. Commit/PR, Jira transition, merge and deployment remain pending.

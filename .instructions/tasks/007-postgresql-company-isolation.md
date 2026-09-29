# Task 007 — Enforce company isolation in PostgreSQL

Status: implemented locally; independent review complete with no P0–P2 findings — approved IAM-002 / OLARIZE-20. Contract: `docs/requirements/organization-identity.md`, rules OI-002/006/007/009. The repository records approval from 2026-09-28; live Jira is unavailable in this session.

## User outcome

A company member's database reads stay within the current verified company, even if an application query omits its company filter. A revoked membership loses access on its next transaction.

## Scope and dependencies

Identity and organization persistence boundaries, PostgreSQL migration and restricted runtime-role tests. Depends on ORG-001, EMP-001 and IAM-001, which are present on `develop`. No employee tables, new HTTP endpoint, new role grants or production onboarding.

## Contract

Before company selection, an authenticated member may discover only their own active memberships. After selection, company reads require transaction-local subject and company context plus a current active membership. The runtime login must not own, bypass RLS or gain migration/provisioning privileges. Context must clear at transaction end, including rollback and pooled reuse. Company-to-membership audit relationships must preserve company IDs. Existing API authentication and administration rules remain in force.

## Acceptance checks

- Missing subject/company context denies company rows and direct writes under the actual restricted runtime role.
- Own-subject discovery returns active memberships only; a forged API subject cannot select another identity.
- Company A context cannot read B rows through direct reads, joins or aggregates; revoked membership loses company access on the next transaction.
- Cross-company membership/audit references are rejected, including foreign-key checks.
- A pooled connection does not retain A's context after commit or rollback.
- The runtime role remains non-owner, non-superuser and NOBYPASSRLS, with no migration/provisioning role membership.

## Verification

`./scripts/verify-backend.sh` passed: 79 tests, zero failures/errors/skips, backend jar packaged. The signed-scope focused suite passed 28 tests; the direct administrator-forgery test passed in the 19-test company-context run. `docker compose --profile app up -d --build --wait` upgraded the existing local volume out of order without deleting it: Flyway V6, V900 and V901 are successful, the backend is healthy, readiness returns UP, Swagger returns HTTP 200, and the synthetic company and two memberships remain persisted.

## Completion notes

Flyway V6 adds a private scope-signing key, verifies a short-lived subject/company proof in RLS policies, and guards all three membership-administration entry points. Direct runtime SQL with forged custom settings or a forged administrator argument is denied; the runtime cannot execute the unguarded internal functions. Own-subject membership discovery is limited to active rows (and to the selected company after selection). Same-company composite foreign keys guard membership audit references. The application signs its database scope after Keycloak validation, within the read or administration transaction. See `docs/company-isolation.md`, ADR 005 and the updated visual flow. The independent reviewer found no remaining P0–P2 issues after the earlier selected-company and administrator-impersonation findings were fixed. Production key delivery/rotation, role provisioning, backup/restore and administrator MFA remain separate gates.

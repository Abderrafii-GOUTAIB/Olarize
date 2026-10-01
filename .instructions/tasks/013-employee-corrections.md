# Task 013 employee factual corrections — OLARIZE-7 / EMP-007

Status: backend implemented, verified and independently reviewed; frontend pending

## User outcome

HR corrects one identified employee's minimum work facts with a reason, exact confirmation and current employee/episode versions. Prior factual sources remain available for restricted review.

## Scope and dependencies

Approved contract: docs/requirements/employee.md EMP-007, EMP-BR-001–009/018/020 and contextual EMP-LR-003/006/008; live Jira OLARIZE-7 matches. EMP-002 exists on origin/develop. This invocation uses the backend-only olarize-feature scope. UI-DEP/correction form and production privacy/legal procedures remain pending. No statutory calculator, private field extension, payroll snapshot rewriting, account linking or end-date mutation is included. Department/site remain labels; IDs are rejected unknown fields. Reporting and payroll persistence are not implemented yet, so no fictitious tables/flows are added.

## Contract

POST /api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/corrections accepts full minimum work facts, effectiveDate, reason, expectedVersion, expectedEpisodeVersion, idempotencyKey and confirm=true. Requires validated profile.read and current database employee.write. Optional omission clears a field; employee reference is case-insensitively unique with displayed leading zeroes preserved. Company, employee and episode scope are checked through membership and restricted PostgreSQL RLS.

Corrections append immutable source/audit records; employee optimistic version advances each time. Episode version advances only when start changes. Recorded and effective dates are separate. Job/department/site are dated immutable projections selected in directory/detail by asOf (latest effective date, then source version). Upcoming reads use the episode start date. Retroactive corrections supersede source facts for future queries without overwriting prior versions or later dated job facts. Name/contact/reference corrections are administrative current facts. Initial job dates are existing dependent intervals; start changes cannot silently move them. Conflicts identify JOB_INTERVAL, LAST_EMPLOYED_DAY or OTHER_EPISODE. Contract dates remain intact.

Immutable baseline versions ensure original creation retries still return original facts after corrections. PostgreSQL advisory retry lock is the focused native SQL exception; CRUD uses JPA. Equal actor/company/correction keys serialize; mismatched payloads/stale versions/duplicates give409. Business state, reason/actor/time/correlation and retry receipt commit together; audit failure rolls back. Errors use sanitized problems:400/401/403/scoped404/409/500. No general history HTTP endpoint is added; EMP-015 owns it.

## Acceptance checks

- Unicode/normalization, syntax/length and effective-date unit decisions.
- HTTP factual correction, date selection, immutable source evidence, exact retries and versions.
- Invalid/unknown/unconfirmed fields, duplicate reference, unauthorized/revoked and foreign-company/episode access.
- Start conflicts and contract preservation; stale/equal-key/different-key concurrent writes and audit rollback.
- Restricted runtime missing/forged/foreign scope, history hidden from directory readers, pooled scope cleanup and immutable grants.
- Local-only persistent baseline data, generated Swagger example and endpoint documentation.

## Verification

On 2026-10-01, full `./scripts/verify-backend.sh` passed 127 tests, zero failures/errors/skips. After the start/job-date boundary correction, focused `./scripts/verify-backend.sh -Dtest=EmployeeCorrectionTests,CompanyContextIntegrationTests,LocalDocumentationIntegrationTests` passed 47 tests, zero failures/errors/skips. API YAML parsed with Ruby YAML; git diff --check passed. Verified base origin/develop: ca76aa6e222df046c16d54a86125294e461d90cb. Independent findings-only olarize-reviewer inspected the tracked diff, all untracked feature files and actual logs/Surefire evidence; no actionable P0–P3 findings and no additional backend checks required. Temporary verification logs were removed after recording these results; the final focused Surefire reports remain in backend/target.

Persistent local smoke: packaged backend started with local profile on an ephemeral port; Flyway V12/V908 succeeded in existing Compose PostgreSQL. Readback confirmed both success=true and six synthetic baseline versions in the demo company. Generated Swagger returned the correction path, response schema and confirmed example. The temporary backend stopped cleanly. No browser login or live authenticated correction is claimed.

## Completion notes

The authorized backend slice is complete with immutable sources/audit, scoped optimistic corrections, exact retries, contact/date checks, dependent-fact conflicts and dated minimal directory jobs. Review found no actionable issues. UI and production prerequisites remain pending; reporting/payroll integration belongs to future modules. Jira story is not marked complete by this backend-only slice. The user authorized committing the reviewed backend slice on feature/OLARIZE-7. The project post-commit hook publishes its ticket branch and PR into develop; UI acceptance remains pending.

## CI timestamp precision correction

CI exposed nanosecond first-response timestamps differing from PostgreSQL microsecond retry timestamps. Normalize the mutation timestamp to microseconds at the persistence adapter before storing facts and building the initial result. Keep exact HTTP retry assertions and add a deterministic PostgreSQL nanosecond regression. Full `./scripts/verify-backend.sh` passed 128 tests, zero failures/errors/skips on 2026-10-01. The independent reviewer inspected the fix against bbbe7e2 and actual log/Surefire evidence, confirmed the deterministic regression fails on old behavior, and returned no actionable P0–P3 findings. No additional check required.

# Task 009 record or correct contract facts

Status: implemented and reviewed

## User outcome

An HR user records supplied contract facts for a selected employee employment episode and can later correct them without losing the prior version. The API shows the current version and its review state; recording facts does not assess legal validity or change employment status.

## Scope and dependencies

Implement approved EMP-004 / OLARIZE-3 as a backend slice after EMP-002. Accessible create/correction review forms and document attachment storage remain outside this backend-only task. Use synthetic local data only; OD-01/02/04/05 constrain real customer data. OD-07 remains a legal review gate and no legal rules are activated.

## Contract

Accepted recorded type codes: `CDI`, `CDD`, `SPECIFIED_WORK`, `UNKNOWN`. Treat them only as labels HR supplied; each saved version has review state `UNREVIEWED`, and no legal validity, eligibility or renewal conclusion is computed. Routes and schemas are specified per endpoint in `docs/api/employment/` and summarized in `docs/api/openapi.yaml`.

## Acceptance checks

- HR with current `contract.write` can create one versioned contract fact set for an employee episode in the selected company; initial review state is unreviewed.
- Contract facts include a recorded type code, start date, optional end date and source reference. An omitted end date remains explicitly missing; it is never invented.
- Corrections require the current version, a reason and evidence reference, append a new version and preserve the prior facts. Stale corrections conflict.
- Reject invalid type codes, end dates before start dates, unsupported/oversized fields and unconfirmed writes without exposing rejected values.
- Non-HR actors, revoked memberships and cross-company employee/episode/contract identifiers cannot write or reveal protected records.
- Persist the contract version, audit evidence and retry receipt atomically; test concurrent stale corrections and rollback on audit failure.
- Expired contract facts do not end the employment episode or change membership/login state.
- Persistent synthetic local data and meaningful Swagger examples support creating a contract and correcting its seeded version; API fragments live in `docs/api/employment/` with one file per endpoint.

## Verification

- `./scripts/verify-backend.sh` — BUILD SUCCESS; 92 tests, 0 failures/errors/skips; PostgreSQL Testcontainers migrations through V904 and JPA validation passed.
- `git diff --check` — passed.
- Independent `olarize-review` — no remaining findings after aligning accepted contract codes to `CDI`, `CDD`, `SPECIFIED_WORK`, `UNKNOWN`.

## Completion notes

Implemented on `feature/OLARIZE-3`. Create facts with `POST /api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts`; append a correction with `POST /api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts/{contractId}/corrections`. Each endpoint has its own YAML contract in `docs/api/employment/`. Local seeded data and example requests are documented in `docs/employment-contract-facts.md`.

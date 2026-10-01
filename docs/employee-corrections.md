# Employee factual corrections — OLARIZE-7 / EMP-007

The backend accepts one confirmed correction for a stable employee and episode ID. It stores immutable source versions and restricted audit, supports safe retries, and selects job facts by effective date. The accessible correction/review form remains pending UI-DEP.

## API

`POST /api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/corrections`

Requires a valid Keycloak token with `profile.read` and current database `employee.write` membership. Supply the full minimum work facts, `expectedVersion`, `expectedEpisodeVersion`, `effectiveDate`, a nonblank reason, a UUID `idempotencyKey`, and `confirm: true`. Omitted optional facts clear those values. Unknown/private/payroll/end-date/account/reference-ID fields are rejected. Department and site are plain labels. Use employee IDs even when names repeat.

Success returns company/employee/episode IDs, new employee version, episode version, effective date and recorded timestamp. Employee version advances for each correction; episode version advances only if its start changes. Cache-Control is no-store. Equal actor/company/key/payload retries return the original result even after later corrections; mismatched payloads conflict. A creation retry also returns its original version-1 facts after subsequent corrections.

Names, references and work contacts are current administrative corrections. Job title, department and site take effect on `effectiveDate`. Directory/detail `asOf` selects the latest effective job date, then latest source version on that date. A retroactive correction can replace facts on an earlier date without rewriting a later scheduled job assignment. Upcoming reads show facts effective at the recorded episode start. The API stores no legal outcome and never shifts contract dates or frozen downstream evidence.

Errors are sanitized problems with requestId: 400 invalid/unconfirmed/unknown input; 401 unauthenticated; 403 missing/revoked permission; scoped404 unavailable employee/episode; 409 duplicate reference, stale source versions, conflicting retry or dependent facts; 500 persistence/audit outage. Dependent start conflicts add `dependentFacts` codes `JOB_INTERVAL`, `LAST_EMPLOYED_DAY`, `OTHER_EPISODE` for separate correction. An existing initial job is an interval beginning at the supplied original start; it cannot be silently moved by changing employment start. Job changes after a recorded last day also conflict. Reporting relationships and payroll snapshots do not yet exist; their future modules must validate dependent intervals and retain these immutable source IDs/versions. This slice does not claim integration with them.

## Local Swagger example

Start persistent local PostgreSQL and the backend with the local profile using [development setup](development.md). Local-only V908 stores baseline facts and job projections for synthetic employees; shared dev/prod never load this location. Use `olarize-demo-admin`, setting its password outside source as described in [authentication](authentication.md). Its existing company membership grants employee.read/write.

Authorize Swagger at `http://localhost:8080/swagger-ui/index.html`. Read:

- `GET /api/v1/companies/c0000000-0000-4000-8000-000000000001/employees/c0000000-0000-4000-8000-000000000070?asOf=2026-10-01`
- Select the employee correction POST with company `c0000000-0000-4000-8000-000000000001`, employee `c0000000-0000-4000-8000-000000000070`, episode `c0000000-0000-4000-8000-000000000071`.

The generated example submits:

```json
{
  "reference": "EMP-0001",
  "name": "Yasmine Bennani",
  "startDate": "2026-09-01",
  "jobTitle": "Senior HR Coordinator",
  "department": "People Operations",
  "site": "Casablanca",
  "workEmail": "yasmine@demo.olarize.invalid",
  "workPhone": "+212 5 22 00 00 01",
  "effectiveDate": "2026-10-01",
  "expectedVersion": 1,
  "expectedEpisodeVersion": 1,
  "reason": "Reviewed synthetic job assignment",
  "idempotencyKey": "c0000000-0000-4000-8000-000000000090",
  "confirm": true
}
```

Read current versions first if this persistent demo has already been edited. Reuse the exact key/body for a retry; use fresh versions and a new key for a different correction. GET detail with `asOf=2026-09-15` shows HR Coordinator; `asOf=2026-10-01` shows Senior HR Coordinator. No unconfirmed request writes data.

## Storage and verification

V12 introduces immutable `employment_employee_version` and `employment_job_version` with composite company/employee/episode foreign keys, RLS and limited runtime grants. History contains only approved minimum facts plus actor/reason/correlation, visible only with employee.write. Directory readers see the separate allowlisted job projection. JPA optimistic updates prevent lost changes; a focused PostgreSQL advisory transaction lock serializes equal retry keys. Audit/version insertion failure rolls back the whole transaction.

Run `./scripts/verify-backend.sh`. Unit/HTTP/PostgreSQL tests cover validation, effective dates, original and correction retries, stale/duplicate/concurrent writes, start conflicts, contract/source preservation, company scope, history isolation and audit rollback. Production data-processing/retention reviews and UI acceptance remain pending.

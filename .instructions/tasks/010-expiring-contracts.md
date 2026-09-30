# Task 010 list contracts with recorded expiry in a date range

Status: implemented and verified

## User outcome

An authorized HR user can find current contract records whose HR-recorded end dates fall within a chosen date range, including the employee reference and contract version needed to verify each result.

## Scope and dependencies

Implement approved EMP-005 / OLARIZE-4 after EMP-004. This read-only backend API does not send notifications, infer renewals, determine legal validity, or change employment or payroll state.

## Contract

`GET /api/v1/companies/{companyId}/contracts/expiring` requires `fromDate` and `toDate` (inclusive, at most 366 days), with optional zero-based `page` (default 0) and `pageSize` (default 25, maximum 100). Requires Keycloak `profile.read` and current company `contract.read`. It returns only each contract's current version, ordered by recorded end date then contract ID, plus a separately scoped count of current contracts with no recorded end date. An authorized empty result is a successful response with an empty `items` list. Date selection uses `Africa/Casablanca`; no end date remains unknown and is not counted as expiring.

## Acceptance checks

- Include both date boundaries; reject missing/invalid dates, inverted or overlong windows, duplicate/unknown query fields, and invalid pagination without an unbounded query.
- Include a contract only when its current version's recorded end date is inside the window. Keep separate contracts for the same employee as separate results.
- Require current `contract.read` membership and signed selected-company PostgreSQL scope; exclude other companies from results, totals and missing-end-date coverage.
- Return stable ordering, bounded pagination, employee name/reference, contract/employee IDs, current version and recorded start/end dates.
- Persist local synthetic data for Swagger; document exact calls and create one YAML API fragment for this endpoint.
- Update `docs/features-and-flows.md` with the feature map, flow, status and verification.

## Verification

- `./scripts/verify-backend.sh` — BUILD SUCCESS on 2026-09-30; 97 tests passed, zero failures/errors/skips. Includes PostgreSQL/Testcontainers, HTTP, local Swagger and architecture checks.
- Ruby YAML parsing for all `docs/api/**/*.yml` and `docs/api/**/*.yaml` — passed.
- `git diff --check` — passed.
- Independent `olarize-review` and focused structural re-review — no material findings. The remaining item-schema naming inconsistency was corrected.

## Completion notes

EMP-005 implementation and documentation are verified. Identity and employment web adapters now use separate controller, request/response DTO, mapper and configuration packages. Transport names use `RequestDto`/`ResponseDto`; JSON fields and endpoint URLs remain unchanged. Dedicated unit tests cover the date-window domain rules; HTTP/PostgreSQL integration tests exercise the application service and authorization. No dedicated expiry service unit test currently exists.

Structural follow-up: the approved infrastructure naming and persistence subfolders are applied to employment, identity and organization. Full backend verification passed again after the move (97 tests); independent structural review found no actionable issues.

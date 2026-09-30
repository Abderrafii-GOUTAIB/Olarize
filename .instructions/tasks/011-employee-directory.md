# Task 011 employee directory and detail

Status: backend implemented and verified; frontend pending UI-DEP

## User outcome

Authorized HR users can search company employees and inspect the correct record by its stable reference and ID. Implements the backend slice of approved EMP-003 / OLARIZE-6; the accessible frontend remains gated by UI-DEP under the current backend-only phase.

## Scope and dependencies

EMP-002, trusted company membership and PostgreSQL isolation are implemented. Use approved local requirements because Jira MCP tools are unavailable in this session. No bulk export, private contact fields, manager projection, mutations, AI or frontend work.

## Contract

GET `/api/v1/companies/{companyId}/employees`: optional literal name/reference `search`, `status` CURRENT (default), UPCOMING, ENDED or ALL, optional ISO `asOf` (default current Africa/Casablanca date), page 0 default and pageSize 25 default/max100. Stable reference/ID ordering, one selected employment episode per employee and company-scoped total. Select the latest episode started as of the date, otherwise the earliest upcoming episode; same-date ties use episode ID deterministically. End dates are inclusive. Contract expiry never determines employee status.

GET `/api/v1/companies/{companyId}/employees/{employeeId}`: same optional asOf, minimal employee/work facts and selected episode/status. Require current `employee.read` and Keycloak `profile.read`. A foreign/missing employee returns scoped404; invalid input400, unauthenticated401, unauthorized403, database failure sanitized500 with request ID. Both responses disable caching.

## Acceptance checks

- Duplicate names remain separate references; literal name/reference search and stable bounded pagination/counts.
- Inclusive status dates, ended/all filters, explicit asOf and deterministic episode selection.
- Company isolation for search, detail, totals and PostgreSQL signed context; current revocation/permission checks.
- Nonempty local Swagger results from persistent synthetic seeds; endpoint-specific YAML fragments and descriptions.
- Unit tests for domain filters/status and service denial; PostgreSQL/HTTP tests for filters, failure and scopes.
- Update features-and-flows diagrams and independently review after full backend verification.

## Verification

- Full `./scripts/verify-backend.sh`: 106 tests, zero failures/errors/skips; BUILD SUCCESS on 2026-09-30.
- After review pagination fix: `./scripts/verify-backend.sh -Dtest=EmployeeDirectoryFilterTests,CompanyContextIntegrationTests` passed 33 tests with no failures/errors/skips.
- Checked-in OpenAPI and both endpoint fragments parse as YAML; `git diff --check` passes.
- Independent review identified the JPA offset limit; validation and domain/HTTP 400 regressions now cover it. Focused re-review returned no outstanding actionable findings.
- Live Keycloak/browser interaction was not exercised; signed HTTP and persistent local-profile migration/Swagger checks passed.

## Completion notes

Backend includes company-scoped search/detail, separate DTOs/mappers, JPA queries, RLS read policies, local synthetic employees, endpoint fragments and the feature flow diagram. Defaults and episode selection are recorded above. Frontend acceptance and live-production privacy/hosting gates remain separate.

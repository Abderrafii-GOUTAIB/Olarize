# Task 008 minimum employee record

Status: implemented and independently reviewed

## User outcome

An HR user records a person's minimum identity and employment start for a selected company and receives stable IDs, reference, dates and version. A future start appears as UPCOMING; recording the fact does not create a login or payroll eligibility.

## Scope and dependencies

Implement approved EMP-002 / OLARIZE-2 as an employment backend slice after OLARIZE-20. UI-DEP and an accessible create/review form remain a separate prerequisite because the product frontend has not been built. Use synthetic local data only; OD-01/02 govern real customer data.

## Contract

POST /api/v1/companies/{companyId}/employees accepts explicit confirmation, a UUID idempotency key, reference, name, start date and optional work facts. Only an active membership with employee.write may create. The URL company is a requested scope, never authority; companyId in the body is unsupported. Creation persists employee, first episode, retry receipt and audit atomically. Same-key/same-payload retries return the original result; conflicting keys or references return 409. Invalid fields return 400 without echoing values. No account, contract, compensation or import is created.

## Acceptance checks

- Confirmed HR creation returns stable IDs, reference, dates, derived status and version.
- Invalid dates, unknown/invalid fields, unauthorized actors and wrong-company requests return sanitized errors and do not write.
- Same-key retry is stable; changed payload conflicts even when text fields contain line breaks; same reference in another company is allowed.
- Simultaneous same-key requests create one employee, episode and audit; simultaneous duplicate references produce one success and one conflict.
- Audit failure rolls back the business write; PostgreSQL RLS blocks unscoped or foreign-company reads/writes.

## Verification

Verified 2026-09-30 with `./scripts/verify-backend.sh`: 85 tests passed, zero failures, errors or skips. This includes 23 signed-token/PostgreSQL company-context integration tests (the EMP-002 HTTP, RLS, retry, collision, rollback and concurrency cases), 2 pure domain tests, 3 sanitized HTTP error tests, 2 local Swagger/profile tests and 2 Modulith/architecture tests. The backend package built successfully. Local Compose also started healthy; readiness and Swagger returned HTTP 200, generated OpenAPI included the employee endpoint/example, and PostgreSQL retained synthetic `EMP-0001` with its first episode. Independent review findings were fixed; the reviewer’s Java 21 `HttpRequest.Builder.copy()` concern was disproved by the successful compile and focused 23-test run on the configured JDK.

## Completion notes

Implemented on `feature/OLARIZE-2`. No commit or PR has been created. The UI-DEP structured form remains a separate prerequisite; production role provisioning, privacy and hosting gates remain open for real employee data.

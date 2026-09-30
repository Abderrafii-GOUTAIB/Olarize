# Minimum employee creation (EMP-002)

`POST /api/v1/companies/{companyId}/employees` records a supplied employee name, company-unique reference and first employment start date after explicit `confirm: true`. Optional work labels and contact fields are allowed. The response returns immutable employee and episode IDs, the reference, date, version 1 and `UPCOMING` or `CURRENT` as of the current day in Africa/Casablanca. No Keycloak account, contract, payroll eligibility or legal hiring decision is created.

The caller needs a valid `profile.read` access token and a current `employee.write` membership in the selected company. A reference is unique after trimming and case folding within that company; identical names remain distinct. A UUID `idempotencyKey` makes a retry with the exact same facts return the same IDs. Reusing it with different facts or using an existing reference returns 409. Employee, first episode, retry receipt and audit commit in one transaction. PostgreSQL forces RLS on all four tables, and a signed company scope plus current HR membership is required.

## Local Swagger example

The local Flyway migration `V902__local_employee_demo.sql` persists a synthetic employee (`EMP-0001`, Yasmine Bennani) in the local PostgreSQL volume. `V903__local_demo_admin_hr_permission.sql` explicitly gives the existing `olarize-demo-admin` test account both identity-admin and HR permissions, then records that local grant. The seeded user ID is `c0000000-0000-4000-8000-000000000011`. Set or reset its password in the local Keycloak Admin Console under the `olarize` realm; no password is committed. Keep the existing database volume.

1. Sign in to Swagger at `http://localhost:${BACKEND_PORT:-8080}/swagger-ui/index.html` as `olarize-demo-admin` using Authorize. The current local `.env` may choose a different `BACKEND_PORT`.
2. Call `GET /api/v1/identity/memberships` and use the company ID `c0000000-0000-4000-8000-000000000001`. The demo administrator's membership lists both `identity.admin` and the explicitly granted `employee.write`.
3. Call `POST /api/v1/companies/c0000000-0000-4000-8000-000000000001/employees` with the example body in Swagger. Use a new reference such as `EMP-0007` and a fresh UUID idempotency key for each new employee. A successful call returns a nonempty created employee response; repeating the exact body returns the same IDs.

The seeded `EMP-0001` verifies that company data persists across restarts; the directory and detail read API belongs to EMP-003 and is not exposed by this story. Real customer data remains blocked by the documented privacy, role-provisioning and hosting gates. The accessible create/review form depends on UI-DEP, which is not yet implemented.

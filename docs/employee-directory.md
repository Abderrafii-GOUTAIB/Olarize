# Employee directory — OLARIZE-6 / EMP-003

HR can find a company employee and inspect the correct record. This is the backend API slice; the accessible frontend required by UI-DEP remains pending.

Both endpoints require a Keycloak bearer token with `profile.read` and an active membership containing `employee.read` in the selected company. `companyId` selects the company; it does not grant access. Use the existing `olarize-demo-admin` login for local Swagger, following [authentication](authentication.md). No password is committed.

## 1. Search employees

`GET /api/v1/companies/{companyId}/employees`

Example using company `c0000000-0000-4000-8000-000000000001`:

```text
/api/v1/companies/c0000000-0000-4000-8000-000000000001/employees?search=Ahmed&asOf=2026-09-30
```

Returns two separate Ahmed El Amrani records: `EMP-0002` in Finance and `EMP-0003` in Operations. Choose by reference and employee ID, rather than name alone. The response includes `companyId`, `asOf`, `timeZone`, `items` and `pagination` with page, pageSize, totalItems and totalPages. Summary rows exclude work contact fields.

| Parameter | Behavior |
|---|---|
| `search` | Literal partial name or reference, case insensitive, maximum 200 characters. Blank means no filter. `%` and `_` are literal characters. |
| `status` | `CURRENT` by default; `UPCOMING`, `ENDED` or `ALL` explicitly. |
| `asOf` | ISO date, default today in Africa/Casablanca; returned explicitly. |
| `page` | Zero based, default 0; page × pageSize must not exceed 2,147,483,647. |
| `pageSize` | 1–100, default 25. |

Ordering is normalized employee reference then employee ID. Counts use the same company and filters as the page. An authorized search with no matches returns empty items; a database outage returns an error.

## 2. Inspect an employee

`GET /api/v1/companies/{companyId}/employees/{employeeId}`

```text
/api/v1/companies/c0000000-0000-4000-8000-000000000001/employees/c0000000-0000-4000-8000-000000000080?asOf=2026-09-30
```

Returns Ahmed `EMP-0002`, job title Accountant, department Finance, site Casablanca, recorded work contacts when available, record versions and the selected employment episode. Optional `asOf` has the same behavior as search. `employeeId` identifies the employee; `episodeId` identifies one period of employment for that employee. Neither is a Keycloak user ID.

Private contact, identity documents, salary and document contents are excluded. Both successful endpoints use `Cache-Control: no-store`.

## Employment status

Select the latest episode that has started as of the date. If none has started, select the earliest upcoming episode. Same-date ties use episode ID deterministically. An employee appears once. The start date and last employed day are included: CURRENT through the last employed day, ENDED the following day, UPCOMING before the start. A contract's expiry does not end an employment episode.

## Persistent local demo records

`V906__local_employee_directory_demo.sql` adds four synthetic records to the persistent local database. Existing Yasmine remains from V902. Flyway applies these migrations once; database volume storage retains them across app restarts. Shared and production profiles do not load local seed migrations.

| Reference | Name | Employee ID ending | Episode ID ending | Status on 2026-09-30 |
|---|---|---|---|---|
| EMP-0001 | Yasmine Bennani | 070 | 071 | CURRENT |
| EMP-0002 | Ahmed El Amrani | 080 | 081 | CURRENT |
| EMP-0003 | Ahmed El Amrani | 082 | 083 | CURRENT |
| EMP-0004 | Nadia Berrada | 084 | 085 | ENDED (last day 2025-12-31) |
| EMP-0005 | Omar Alaoui | 086 | 087 | UPCOMING (starts 2099-01-01) |

All these IDs start with `c0000000-0000-4000-8000-000000000`. With `asOf=2026-09-30`: no search and default status returns 3 current employees; `search=Ahmed` returns 2; `status=ENDED` returns Nadia; `status=UPCOMING` returns Omar; `status=ALL` returns 5. Work contacts on the four new records are absent and returned as null rather than invented contact data.

## Errors and verification

400: malformed filters, IDs or date, invalid pagination, unknown/duplicate parameters. 401: invalid/missing bearer token. 403: missing current company permission. Detail 404: absent or foreign-company employee, with the same response. 500: database failure, sanitized problem with requestId. Revoked membership or reduced permissions apply on the next request using the same token.

Verification: `./scripts/verify-backend.sh` runs domain and service unit tests, restricted PostgreSQL/RLS HTTP integration tests, local migration/Swagger checks, architecture checks. Tests cover duplicate names, literal search, inclusive dates, pagination, scoped counts/detail, revoked access and database failure. JPA handles employee queries; JDBC is limited to the existing signed transaction-local database scope operation.

Endpoint contracts: [search](api/employment/get-list-employees.yml) and [detail](api/employment/get-employee-detail.yml), also included in [OpenAPI](api/openapi.yaml).

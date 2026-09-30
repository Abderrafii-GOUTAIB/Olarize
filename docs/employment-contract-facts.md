# Record and correct contract facts (EMP-004)

EMP-004 records contract facts HR supplies for one employee employment episode. Each version stores a type code, start date, optional end date, source reference, actor, recorded time and review state. Corrections also store a reason and evidence reference. The prior version remains available in the database.

The accepted codes are `CDI`, `CDD`, `SPECIFIED_WORK` and `UNKNOWN`. They are HR-reported labels only. Every saved version is `UNREVIEWED`; the API does not decide legal validity, renewal, work eligibility, payroll eligibility, expiry consequences or employment/account state. When `endDate` is absent, the response says `endDateCompleteness: NOT_RECORDED`. It does not infer a permanent contract or invent an expiry date. Source and evidence references are labels; these endpoints do not upload files.

## Local Swagger example

The local-only Flyway migration `V904__local_contract_demo.sql` persists a synthetic contract for `EMP-0001`, Yasmine Bennani. Keep the local PostgreSQL volume. The fixed demo IDs are:

- Company: `c0000000-0000-4000-8000-000000000001`
- Employee: `c0000000-0000-4000-8000-000000000070` (`EMP-0001`)
- First employment episode: `c0000000-0000-4000-8000-000000000071`
- Seed contract to correct: `c0000000-0000-4000-8000-000000000073`, version `1`

1. Start the local stack and sign in to `http://localhost:${BACKEND_PORT:-8080}/swagger-ui/index.html` as the synthetic `olarize-demo-admin` user. Its local membership includes `contract.write`; set its password using [local authentication](authentication.md).
2. To create a separate contract, call `POST /api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts` with the IDs above and the Swagger example. Use a fresh UUID idempotency key. Copy the returned `contractId` and version.
3. To correct the seeded contract, call `POST /api/v1/companies/c0000000-0000-4000-8000-000000000001/employees/c0000000-0000-4000-8000-000000000070/episodes/c0000000-0000-4000-8000-000000000071/contracts/c0000000-0000-4000-8000-000000000073/corrections` with `expectedVersion: 1`, a reason and an evidence reference. A successful response returns version 2. Repeating the identical request returns that same version. A new correction must use the latest version and a new idempotency key.

The endpoint-specific OpenAPI fragments are [`post-create-contract.yml`](api/employment/post-create-contract.yml) and [`post-correct-contract.yml`](api/employment/post-correct-contract.yml). The API uses a valid Keycloak `profile.read` token and rechecks current company membership and `contract.write` on each write. Cross-company references return an unavailable response without identifying the foreign record.

Real customer data remains blocked until applicable privacy, role-provisioning and hosting gates are satisfied. The accessible create/correction form and contract document storage remain separate work.

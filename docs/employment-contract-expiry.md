# List contracts with recorded expiry dates (EMP-005)

EMP-005 lets a company HR user find the current contract facts whose recorded end dates fall inside an inclusive date range. The API includes employee name/reference, contract and employee IDs, current contract version, start/end dates, and bounded pagination. It does not send notices, infer renewals, assess legal status or change employment/payroll state.

The request requires `fromDate` and `toDate` in `YYYY-MM-DD` format. The range can span at most 366 inclusive days. Optional `page` starts at zero and defaults to `0`; `pageSize` defaults to `25` and cannot exceed `100`. Results are ordered by end date and then contract ID. The response includes the local `Africa/Casablanca` date when built and a company-scoped count of current contract records with no recorded end date. This count is separate from expiring results and does not imply a future expiry.

## Try it in local Swagger

1. Start the local stack and authorize Swagger as `olarize-demo-admin` using its `profile.read` scope. The local account has current `contract.read` access to Demo Morocco SARL.
2. Use the company ID returned by `GET /api/v1/identity/memberships`: `c0000000-0000-4000-8000-000000000001`.
3. Call `GET /api/v1/companies/c0000000-0000-4000-8000-000000000001/contracts/expiring?fromDate=2027-01-01&toDate=2027-12-31`.
4. The persistent local seed returns contract `c0000000-0000-4000-8000-000000000073`, version `1`, for Yasmine Bennani (`EMP-0001`), with the recorded end date `2027-08-31`. It is stored by local Flyway migration `V904__local_contract_demo.sql` and remains in the local database volume across application restarts.

If no contracts match an authorized range, the endpoint returns `200` and `items: []`. Missing or invalid dates, reversed or overlong ranges, unsupported/duplicate query parameters, invalid page values, or page sizes outside 1–100 return `400`. The operation requires a valid Keycloak token with `profile.read` and current `contract.read` permission in the requested company; the company ID by itself does not grant access.

The endpoint contract is [`docs/api/employment/get-expiring-contracts.yml`](api/employment/get-expiring-contracts.yml). PostgreSQL read policies verify the signed company scope and current permission for contracts, versions, employees and episodes.

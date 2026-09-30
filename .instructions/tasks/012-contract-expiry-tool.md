# Task 012 contract expiry assistant backend — OLARIZE-5 / EMP-006

Status: backend slice implemented, verified and independently reviewed; frontend and live provider pending

## User outcome

HR asks a supported expiry question and receives the actual authorized contract result, with explicit dates, empty/clarification/refusal/error states. The user explicitly authorized this backend tool slice on 2026-09-30; this is a narrow exception to the deferred assistant phase and olarize-feature's usual exclusion. No external model, RAG, write tool or frontend is activated. Approved local EMP-006 requirements supply ticket metadata because Jira tools are unavailable.

## Scope and dependencies

Use EMP-005's public listExpiringContracts service; never query employment repositories. A replaceable intent port receives only a bounded question, never HR results, company or identity context. A local-only deterministic provider recognizes reviewed English/French/Arabic examples; other profiles report provider unavailable. It is a synthetic evaluation adapter, not a production language model. The provider has a two-second deadline, bounded workers/queue, no retries, one plan and at most one tool invocation per request.

## Contract

POST /api/v1/companies/{companyId}/assistant/conversations creates owned metadata (no text/history), valid for 24 hours. POST /api/v1/companies/{companyId}/assistant/conversations/{conversationId}/contract-expiry-requests accepts question (1–1000 characters) and bounded pagination (25 default, max100). Both require current contract.read and profile.read. Conversation ownership, company, expiry and current permissions are checked with restricted PostgreSQL RLS. Missing/foreign/expired conversations give scoped404.

Only listExpiringContracts is allowlisted. NEXT_MONTH is resolved by an injected Africa/Casablanca Clock, including year boundaries. Explicit date arguments are validated; ambiguous questions clarify without querying. Unknown tool/context override arguments are rejected before querying. The service result alone supplies names/counts/versions. Provider timeout/unavailable returns sanitized503; tool/database failure remains500 with requestId and documented structured API fallback. Successful responses disable caching. Diagnostics measure outcome/duration without questions or HR payloads.

## Acceptance checks

- Unit multilingual/month/year/timezone/ambiguity/injection evaluations and bounded provider failures.
- Service tests show ownership and permissions checked before provider/query and no invented results.
- Real PostgreSQL/HTTP conversation-to-result test, foreign-company/other-owner/expired/revoked cases, missing context/forged RLS, invalid input and errors.
- Persistent local demo conversation/contract with exact Swagger calls; dedicated endpoint YAML, diagrams and architecture scope note.
- Full backend verification and independent review.

## Verification

Full `./scripts/verify-backend.sh` passed 117 tests, zero failures/errors/skips, on 2026-09-30. Endpoint fragments and checked-in OpenAPI parse as YAML; diff whitespace checks pass. Focused `./scripts/verify-backend.sh -Dtest=CompanyContextIntegrationTests` passed 32 tests including direct ownership, foreign-company and forged-context RLS checks. Independent review against base 6ac179d returned no actionable P0–P3 findings.

## Completion notes

Owned metadata, bounded intent planning, allowlisted read-only tool, actual service results, local seed, separate DTOs/mappers/JPA persistence and endpoint docs are implemented. Conversation UI, live Keycloak/browser validation, general language understanding, live model/OD-05 and production metadata deletion/retention remain pending; no real personnel data is sent externally.

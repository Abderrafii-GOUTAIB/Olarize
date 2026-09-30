# Contract expiry assistant backend — OLARIZE-5 / EMP-006

HR can start an owned conversation and ask a supported expiry question. Results come from the existing authorized employment service and PostgreSQL. The user authorized this backend slice on 2026-09-30. The conversation frontend and live model integration remain pending.

## Swagger: two endpoints

Log in as `olarize-demo-admin` following [authentication](authentication.md). Use company `c0000000-0000-4000-8000-000000000001`.

### 1. Start a conversation

`POST /api/v1/companies/{companyId}/assistant/conversations`

No request body. Returns 201 with `conversationId`, `companyId` and `expiresAt`. Copy the returned conversation ID into endpoint 2. It belongs to the current user and company and expires after 24 hours. Another HR user cannot use it. A retry creates a new conversation; this does not change any employee or contract.

### 2. Ask for expiries

`POST /api/v1/companies/{companyId}/assistant/conversations/{conversationId}/contract-expiry-requests`

For a stable nonempty example independent of today's month:

```json
{
  "question": "Which contracts expire in October 2026?",
  "page": 0,
  "pageSize": 25
}
```

The initial demo returns `status=RESULTS`, Yasmine Bennani (`EMP-0001`), contract `c0000000-0000-4000-8000-000000000090`, end date `2026-10-15`, version 1. `result` includes the actual date window, as-of date, timezone, items, page, pageSize, totalItems, totalPages and missing-end-date count. Corrections or additional records can change results. `fallbackPath` identifies the ordinary structured expiry API.

Reviewed relative-date examples:

- English: `Which contracts expire next month?`
- French: `Quels contrats expirent le mois prochain ?`
- Arabic: `ما هي العقود التي تنتهي الشهر المقبل؟`

On 2026-09-23 in Casablanca these resolve to 2026-10-01 through 2026-10-31. December crosses into January of the next year; timezone rules handle Casablanca midnight. On other dates the requested month changes, so a genuine no-match result can be empty. The local synthetic planner recognizes these exact examples (case/outer whitespace normalized); it does not offer general natural-language understanding.

## Responses and fallback

| Result | Meaning |
|---|---|
| RESULTS | Actual matches exist; this page may be empty if beyond the last page. |
| EMPTY | The authorized query found zero matching contracts. |
| CLARIFICATION_REQUIRED | Unknown wording or ambiguous period; no employment query runs. Specify dates using the structured fallback. |
| REFUSED | A provider explicitly refuses unsupported work; no employment query runs. |
| 400 | Invalid question/pagination/JSON fields or invalid untrusted tool arguments. |
| 401 / 403 | Invalid authentication / missing current company `contract.read`. |
| 404 | Conversation absent, expired, wrong company or different owner. |
| 503 | Provider disabled, timed out, failed or saturated. |
| 500 | Database/tool failure with sanitized problem and requestId. |

For ambiguity or provider failure, use existing `GET /api/v1/companies/{companyId}/contracts/expiring?fromDate=2026-10-01&toDate=2026-10-31`. This fallback has the same permissions. No confirmation is needed for a read. Successful assistant responses use `Cache-Control: no-store`.

Questions contain no company/user fields. The URL company is checked against current membership; owner identity comes from Keycloak. Provider output cannot override company or subject, invoke arbitrary tools, perform writes or produce employee names. Only `listExpiringContracts` can run, once per request. The service rechecks ownership/permission after planning, and the employment service applies current authorization and RLS again.

## Provider and storage boundaries

The local profile alone enables the deterministic evaluation provider. Shared dev/prod (including mixed prod+local) report 503; there is no live provider, model SDK, external request, RAG or HR write tool. The replaceable intent port receives only the bounded question, never database results or identity/company context. Two workers, a queue of four, a two-second deadline and no retry bound planning resources. Page size defaults to 25/max100; page offsets must fit the JPA limit. Each request has one planner call and at most one HR tool call. Logs record outcome and duration with request correlation; they omit questions, identities and HR records. Synthetic planning consumes no provider tokens or billed model usage. These bounds are not live model performance evidence.

`assistant_conversation` persists only ID, company, owner and creation/expiry instants. No transcript or result history is stored. RLS requires signed company context, current contract.read, matching owner and unexpired metadata. Expired metadata is inaccessible; operational deletion/retention policy remains part of the production conversation gate. Runtime can select/insert metadata but cannot update/delete it. Live processing remains subject to OD-05 and ADR 003.

## Persistent demo data

Local-only V907 adds Yasmine's synthetic October 2026 contract and version to the persistent database, alongside existing demo records. Flyway applies it once; restarting the app preserves it in the database volume. It adds no reusable conversation ID: create your own fresh conversation so ownership and expiry remain valid. Shared environments do not load local seeds.

Contracts and employee names are read from that database, not copied from a mocked tool result. The deterministic provider only selects the tool/date intent. All recorded facts remain UNREVIEWED; expiry does not imply renewal, termination or legal validity.

Contracts: [create conversation](api/assistant/post-create-conversation.yml), [ask expiry](api/assistant/post-contract-expiry-request.yml). Full UI, general language coverage and live model validation are separate acceptance work.

# Task 006 — Administer company memberships

Status: implementation and independent review complete — approved IAM-001 / OLARIZE-19. The live Jira description was fetched on 2026-09-28 and matches `docs/requirements/organization-identity.md`.

## User outcome

A company identity administrator can grant, revise and revoke ordinary member access for a verified Keycloak subject. A removed permission or membership stops working on that user's next request.

## Scope and dependencies

Identity module backend only. Reuse ORG-001 company ownership and EMP-001 membership, authentication and company context. Support the HR administrator, manager, employee and payroll operator permission bundles. Do not grant or change identity-administrator access through this API. No invitation, password management, employee linking, frontend or AI.

## Contract

Provide company-scoped membership listing, exact-change preview and confirmed grant/revision/revocation endpoints. Require the authenticated actor's current active `identity.admin` membership for the selected company. Resolve target subjects through a focused Keycloak adapter for grants/reactivations; revocation remains possible while Keycloak is unavailable. Preview records exact actor, company, target, action and permissions, expires after 30 minutes and is bound to the target/actor versions. Confirmation requires the preview, reason, expected version and idempotency key; write, version increment and before/after audit record are atomic. Recheck actor authorization and target state at confirmation. Reject self-management, cross-company references, unknown bundles/permissions, identity-admin targets, stale/expired previews and conflicting retries. Keep server-side errors sanitized.

## Acceptance checks

- A current company identity admin can preview and confirm a verified ordinary subject's grant, role change, reactivation or revocation.
- Cancelled/unconfirmed previews do not change memberships; stale, expired or mismatched previews cannot be confirmed.
- Ordinary members, self-management, identity-admin delegation and cross-company target references are rejected without writes or data disclosure.
- Role bundles map to fixed versioned permission allowlists; HR does not implicitly receive payroll permissions.
- Provider outage blocks grants/reactivations but does not block ordinary membership revocation.
- Membership and before/after audit persist atomically; audit failure rolls back. Identical idempotent retries return the committed result; conflicting key reuse fails.
- Signed-token HTTP/PostgreSQL tests verify current actor authorization, immediate permission reduction/revocation, company isolation, conflicts and concurrent changes.

## Verification

Run focused domain and integration tests, then `./scripts/verify-backend.sh`. Add OpenAPI documentation and align `docs/api/openapi.yaml` and operational documentation. Record actual results here.

**2026-09-29:** `./scripts/verify-backend.sh` passed: 76 tests, 0 failures, 0 errors, 0 skipped. The 16 `CompanyContextIntegrationTests` cases use Testcontainers PostgreSQL, signed Keycloak-style HTTP tokens and a synthetic Keycloak admin server; they cover grants, role changes, reactivation, scope, preview/confirmation, provider outage, audit rollback, idempotent retries, concurrent grants and next-request permission changes. The jar packaged successfully. Independent review found a P2 gap for update/reactivation coverage; both flows were added, and focused re-review confirmed the gap is closed with no remaining finding. Production identity-administrator authority remains gated by OD-03; no live Keycloak or production check was run.

**2026-09-29 API naming update:** The public identity response uses `currentUserId`. Company membership rows and preview requests/responses use `userId`, so the selected row's value can be copied directly into a preview. Swagger explains the `idempotencyKey` retry behavior. `./scripts/verify-backend.sh -Dtest=FoundationIntegrationTests,CompanyContextIntegrationTests,LocalDocumentationIntegrationTests` passed: 40 tests, 0 failures or errors. The running IntelliJ backend needs a restart to serve the revised JSON contract.

**2026-09-29 persistence naming:** Identity and organization persistence classes now end in `JdbcRepository`; shared membership SQL row mapping is in `MembershipJdbcMapper`. Focused membership/company integration checks passed (24 tests), and `./scripts/verify-backend.sh` passed the final revision (77 tests, 0 failures or errors). Independent focused review found no persistence wiring or mapping issue.

## Completion notes

Production identity-administrator grant authority remains blocked by OD-03. Tests use synthetic subjects and provider fixtures. Jira metadata is recorded in `.github/jira/OLARIZE-19.json` for the approved PR workflow.

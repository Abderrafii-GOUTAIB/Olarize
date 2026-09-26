# Security contract

Authenticate using Keycloak-issued access tokens. Validate RS256 signature, expected issuer, expiration, required subject, API audience, authorized client (azp), access-token type (typ=Bearer) and required scopes. Use standard Spring Security resource-server support. Keep provider-specific claims inside the security adapter. Do not treat an ID token as API authorization. Resolve app memberships from the database; a client-provided company ID is only a requested context until validated.

The SPA uses authorization code flow with PKCE. Prefer short-lived access tokens held in memory; never put tokens in URLs or logs. Document renewal behavior and avoid persistent browser token storage by default. If a backend session/BFF is adopted later, use Secure, HttpOnly cookies and explicit CSRF protection; do not silently mix the two designs.

Every company-owned operation needs a trusted company scope. Scope reads, joins, aggregates, exports, jobs, files and RAG searches. UUID unpredictability is not authorization. Composite constraints and database policies can add defense in depth but do not replace service checks. Test cross-company relationships, not only direct lookups.

Managers access only authorized reports and decisions; attendance views reveal availability without unnecessary medical reasons. Payroll, employer costs and CNSS/CIMR breakdowns require explicit HR/payroll permissions and must not be exposed through aggregate queries or conversational follow-ups to ordinary employees. Employees see their own permitted information. HR rights are company-scoped. Technical cloud access is not automatically HR access. Document downloads require current authorization; use short-lived signed URLs and never expose public buckets.

Persist who changed what and when, with a reason and request correlation where relevant. Avoid secrets, health data, full prompts and document contents in telemetry. Set retention and deletion workflows for conversation text and derived indexes. Do not store medical attachments in a broadly accessible employee record.

Secrets remain outside source and fixtures. Use IAM roles, TLS, encryption and restricted networking. Test backup recovery and access revocation. Flag unresolved customer hosting and international processing requirements before production activation; the architecture is not legal certification.

## Accepted MVP customer-data boundary

Follow ADR 003 in `docs/decisions/003-mvp-data-protection-and-hosting.md`. Managed Moroccan hosting is the target; a provider and verified data locations are still pending. Map business records, files, identity data, logs, backups, email/monitoring integrations, support access and subprocessors before promising residency. Local hosting alone is not a compliance claim.

The initial MVP must not send employee records, personnel/payroll documents or confidential company information to external chat, model or embedding providers. Keep production credentials/connections/dumps and real HR documents out of coding agents, the developer workbench, Jira, GitHub and ordinary tests. Use synthetic data and separate environments. Do not assume that a policy document or embedding is anonymous.

For company-owned PostgreSQL tables, reinforce application authorization with row-level security. Use a runtime role that is not a superuser, table owner or BYPASSRLS role; evaluate FORCE ROW LEVEL SECURITY for relevant owner access. Migration/backup privileges must be separate. Derive tenant context from verified membership, scope it to the transaction, and clear it on pooled-connection reuse; missing or invalid context denies access. Test cross-company reads, writes, joins, references, aggregates and background jobs using the actual runtime role. Database policies do not replace API/field permissions or storage authorization.

Before real customer onboarding, require verified access controls, administrator MFA, safe logs, documented incident handling, a successful restore exercise, retention/export/deletion procedures and reviewed customer terms/applicable CNDP formalities. Document backup expiry rather than promising immediate erasure from all copies. Synthetic-data development need not wait for these production gates.

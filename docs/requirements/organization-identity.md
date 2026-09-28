# Organization and identity — prerequisites before employee records

Status: **APPROVED and PUBLISHED**, 2026-09-28. User approved all four prerequisite tickets: three new Stories and reuse/refinement of EMP-001. Backlog placement verified. User request: define organization/access prerequisites and place them before employee tickets. Scope is the minimum backend foundation before employee records, not the complete organization, identity, or Olarize V1 backlog.

## Scope, ownership and evidence

- `organization` owns company identity. `identity` owns database memberships, permissions and trusted request scope. Keycloak owns authentication. Employee records remain in `employment`.
- Actors: controlled provisioning operator; company identity administrator; authenticated member. Identity administration alone grants no HR or payroll data access.
- Existing code: `backend/src/main/java/com/olarize/platform/internal/security/SecurityConfiguration.java` validates tokens and denies unimplemented routes; `identity/internal/adapter/in/web/CurrentUserController.java` returns the authenticated subject. The only existing business-database migration enables pgvector. No company or membership implementation exists in these inspected files.
- Existing approved [EMP-001 / OLARIZE-1](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-1) already defines company/membership schema, trusted context, own-membership listing and revocation checks. Its live description was read on 2026-09-28 and remains À faire. Reuse this ticket; do not create an equivalent IAM ticket.
- Excluded here: login rebuild, frontend, invitations, self-registration, department/site management, company hierarchies, billing, company deletion, employee/account links, HR/payroll grants inferred from technical access, AI and deployment. EMP-011 continues to own verified employee/account linking.
- Production gates remain separate: reviewed grant authority, administrator MFA, hosting and privacy procedures, retention/deletion and restore evidence. Synthetic development is allowed; no production onboarding is authorized.

## Sources and rule catalogue

Sources are repository product/security requirements, not newly asserted legal conclusions:

- S1: [product V1](../../.instructions/reference/product-v1.md), scope and current backend-first override.
- S2: [security contract](../../.instructions/reference/security.md), authentication, scope, RLS and audit requirements.
- S3: [architecture](../architecture.md), module ownership and public API boundaries.
- S4: [employee requirements](employee.md), EMP-001, actor matrix, PROV-DEP and OD-03.
- S5: [error contract](../errors-and-debugging.md).
- S6: [ADR 003](../decisions/003-mvp-data-protection-and-hosting.md).

All sources inspected for this draft on 2026-09-28. No statutory rate, deadline, retention period or legal eligibility rule is introduced. Existing customer privacy/CNDP applicability remains **needs legal review** as recorded in employee requirements; this draft does not validate its legal evidence or activate it.

| ID | Rule | Type / applicability | Source / status | Stories |
| --- | --- | --- | --- | --- |
| OI-001 | Server-generated immutable company ID; Unicode company name is a label, never an authorization key. Company registration is controlled operational work. | Product; company setup | S3/S4; proposed bounded setup | ORG-001 |
| OI-002 | Resolve subject from validated Keycloak authentication; resolve membership/permissions from current database state. A supplied company ID is only a request. | Security; every request | S2/S4; required | EMP-001, IAM-001, IAM-002 |
| OI-003 | Identity administrator can manage memberships only in its authorized company. It gains no employee/payroll read permission. Never accept arbitrary permission strings or trust token roles as application grants. | Security; administration | S2/S4; required; delegation limits proposed below | IAM-001 |
| OI-004 | Every mutation commits its audit record atomically, including actor, company, action, target identifier, time, reason and correlation. No tokens or dossier content in logs. | Security; writes | S2; required | ORG-001, IAM-001 |
| OI-005 | Idempotency key and normalized payload identify an operation; same retry returns original result, conflicting payload returns conflict. Expected version prevents lost updates. | Product; writes | S4; required pattern | ORG-001, IAM-001 |
| OI-006 | Runtime database role is neither owner, superuser nor BYPASSRLS. Transaction-local verified context; absent context denies company data. Policies constrain both reads and writes, including related IDs. | Security; persistence | S2/S6; required | IAM-002 |
| OI-007 | Membership discovery is constrained to the authenticated subject before company selection; selected-company access requires active membership. No unrestricted membership bootstrap query. | Security; scope bootstrap | S2/S4; required | EMP-001, IAM-002 |
| OI-008 | Invalid input 400, missing authentication 401, forbidden operation 403, unavailable scoped object 404, stale/duplicate conflict 409. Sanitized errors include request ID. | Product/security; HTTP | S5/S4; required | EMP-001, IAM-001 |
| OI-009 | Revocation or permission reduction applies on the next request even with a still-valid token. No self-escalation, implicit payroll rights or arbitrary cross-company grant. | Security; membership changes | S2/S4; required | EMP-001, IAM-001, IAM-002 |

## Coverage and deferred ownership

| Required capability / workflow | Coverage | Boundary |
| --- | --- | --- |
| Create a company to own future employee data | ORG-001 | Controlled command, no customer signup |
| Validate login | Existing Keycloak foundation | Preserve and regression-test |
| List/select authorized company; reject revoked membership | Existing EMP-001 | Reuse OLARIZE-1 |
| Bootstrap and revoke first administrator | ORG-001 setup contract + EMP-001 provisioning procedure | Controlled operator; no public bypass |
| Grant/revise/revoke ordinary app memberships | IAM-001 | Company-scoped administration |
| Database company isolation and safe pooled reuse | IAM-002 | Existing company/membership tables; future tables require their own policies |
| Audit writes | ORG-001 and IAM-001 | Minimal atomic persistence; no dependency on a future history UI |
| Manager and employee own-record scope | EMP-008, EMP-011, EMP-012 | Employee facts/account links needed; cannot precede employee creation |
| Department/site reference administration | Deferred | Employee V1 allows labels; not a prerequisite |
| UI login/company selector | UI-DEP in employee backlog | Separate frontend scope; still required for full employee stories with screens |
| Privacy/retention, MFA, recovery and production grant authority | Existing release gates | This prerequisite set does not complete these operational workstreams |

## Common acceptance contract

All stories use synthetic fixtures and public module APIs. Unit tests cover rules; PostgreSQL integration tests cover actual persistence, atomic audit, conflicts and denied access. HTTP stories also test real signed-token authentication, sanitized problems and lack of cross-company disclosure. No application tests were run for this requirements-only draft.

Writes require explicit command execution or confirmed API submission; preview/cancellation writes nothing. Membership authorization is rechecked when executing a change. Audit failure rolls back the change. Keys are scoped to actor, company and operation; bootstrap keys are scoped to operator and operation. Repeated keys with different payloads fail. Mandatory name and reason fields are trimmed nonblank Unicode, at most 200 and 1,000 characters respectively; reject unknown fields. IDs are UUIDs; Keycloak subjects are opaque identifiers, never inferred from email/name. Proposed limits are product choices, not legal requirements.

### ORG-001 — Provision a company identity

**Review:** APPROVED on 2026-09-28. **Jira:** [OLARIZE-18](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-18) — published; À faire; no sprint. **Actor/value:** As a controlled provisioning operator, I want to register a company once so later memberships and employees have a stable employer identity.

**Scope:** `organization` schema and public company lookup contract plus a non-HTTP administrative command for company creation. Inputs: company name, reason, idempotency key; operator identity comes from authenticated operational execution, not an arbitrary actor field. Persist generated company ID, name, version and timestamps, with atomic audit. The command uses a dedicated restricted provisioning credential unavailable to the web runtime. Document credential handling and synthetic execution. Initial membership provisioning remains EMP-001; no parallel membership implementation here.

**Dependencies:** Existing backend/PostgreSQL/Flyway foundation only. **Permissions:** explicitly authorized provisioning operator. Ordinary application tokens and runtime database credentials cannot create companies. **Rules:** OI-001/004/005. **State:** absent → company exists; no delete, suspension, rename or onboarding-complete status. Same names may occur; names are not unique legal identifiers.

**Acceptance:**

- Given an authorized operator and valid name, when the confirmed command executes, then return one new company ID/version and persist company plus audit atomically.
- Given a retry with the same key and payload, when executed, then return the original company ID; changed payload with that key fails without another company.
- Given blank/oversized name, missing reason or unknown inputs, when executing, then fail with a sanitized diagnostic and no write.
- Given an ordinary runtime credential or HTTP caller, when attempting company provisioning, then deny without creating any row.
- Given companies A and B, when the public module lookup is called with trusted A context and target B, then expose no B metadata. No public all-company directory is introduced.
- Given audit failure or transaction failure, when creating, then persist neither company nor partial evidence.

**Tests:** unit validation/idempotency; PostgreSQL command tests for success, denied runtime credentials, duplicates, rollback and scoped lookup. **Open decisions:** named production operator/grant process remains OD-03; synthetic operator fixtures do not establish it. No unresolved implementation choice beyond approval of this proposed controlled-command surface.

### EMP-001 — Reuse the existing trusted company-context story

**Review:** existing approved story; dependency clarification APPROVED and synchronized on 2026-09-28. **Jira:** [OLARIZE-1](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-1).

Retain every existing acceptance scenario, membership-list/context API, trusted resolver, membership schema and controlled initial provisioning/revocation procedure in [employee requirements](employee.md#emp-001--resolve-my-authorized-company-context-m0-build-first). Do not create a new IAM alias ticket.

**Approved revision, synchronized on 2026-09-28:** replace its “foundation only” dependency with ORG-001; replace ownership of the minimal company schema with consumption of organization's company ID/public contract. Identity still owns membership persistence and permissions. Controlled first-admin provisioning/revocation uses the same restricted operational boundary and atomic audit as ORG-001; do not grant a normal runtime bypass. This extracts company ownership without deleting acceptance behavior. This revision is synchronized in the local story and Jira issue; other employee descriptions are preserved.

### IAM-001 — Administer company memberships and permissions

**Review:** APPROVED on 2026-09-28. **Jira:** [OLARIZE-19](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-19) — published; À faire; no sprint. **Actor/value:** As a company identity administrator, I want to grant, revise and revoke membership so authorized people can use the company workspace and removed access stops working.

**Scope:** backend membership list, exact-change preview and confirmed create/update/revoke APIs. Reuse EMP-001 persistence/resolver. Target is an existing verified Keycloak subject: validate through a focused provider adapter; provider unavailable or missing subject blocks new grants/reactivation. Revocation must remain possible without the provider. No invitation, password management or employee-account link.

**Permissions:** active identity-administrator membership for the selected company; identity role itself is granted/revoked only by EMP-001's controlled operator procedure. This API cannot grant/revoke identity-administrator rights or change one's own permissions. Proposed assignable role bundles are the existing HR administrator, manager, employee and payroll operator from employee requirements. Map bundles to a versioned explicit permission allowlist: HR never implicitly gains payroll; manager/employee data access still needs EMP-011 links and downstream scope checks. Reject unknown roles/permissions. No generic superuser role.

**Dependencies:** EMP-001 and ORG-001. **Rules:** OI-002–005/008/009. **Data/state:** existing company/subject membership, explicit role bundle/permissions, ACTIVE/REVOKED, version. Revoked membership can be explicitly reactivated, preserving its identity/history. Unique company/subject membership; no duplicate parallel memberships. Audit before/after permission sets; reason and confirmed preview version required. Preview contains exact target/company/permissions/action and expires after 30 minutes; stale actor/target versions require renewed preview.

**Acceptance:**

- Given A's identity administrator and a verified target, when confirming a valid grant preview, then create exactly one active A membership with the reviewed permission set and audit.
- Given a valid edit/revoke preview, when canceled or unconfirmed, then leave permissions unchanged; when confirmed against current versions, then update membership and audit together.
- Given removal of a permission or revocation, when the target makes its next request using an old valid token, then deny the removed operation.
- Given an ordinary member, self-edit, unknown role, attempted identity-administrator grant or modification of an identity-administrator membership, when submitted, then reject with no partial write.
- Given A's administrator references a B membership, when listing/previewing/mutating under A, then reveal no B data and change nothing in B.
- Given stale/expired preview, concurrent membership edit, lost actor access or mismatched company/payload, when confirming, then fail without applying the old proposal.
- Given an unavailable identity provider, when granting/reactivating, then fail explicitly; when revoking an existing ordinary membership, then revoke using current database facts.
- Given repeated identical confirmation, then return the committed result; conflicting reuse or duplicate company/subject creation fails; audit failure rolls back.

**Tests:** unit permission mapping/state/preview rules; signed-token HTTP/PostgreSQL tests for all cases, including concurrent grants and reduced access; provider adapter contract tests. **Approved choices:** role-delegation restriction and controlled identity-admin management approved on 2026-09-28. Production grant authority remains OD-03; prerequisites remain required.

### IAM-002 — Enforce company isolation in PostgreSQL

**Review:** APPROVED on 2026-09-28. **Jira:** [OLARIZE-20](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-20) — published; À faire; no sprint. **Actor/value:** As a company member, I want database operations confined to my authorized scope so a missing query filter cannot expose another employer's data.

**Scope:** integrate RLS with the existing organization/identity paths; non-owner runtime role, separate migration/provisioning privileges, policies for company/membership/audit data created by prerequisite stories, transaction-local subject/company settings, and safe rollback/connection reuse. Document how future employee tables inherit the contract; do not precreate employee tables or claim future modules protected.

**Dependencies:** ORG-001, EMP-001 and IAM-001. **Permissions:** existing authenticated membership rules; RLS adds no role grants. **Rules:** OI-002/006/007/009. **State:** no context → own-subject membership discovery → verified company-scoped transaction → context cleared at completion. Subject derives only from the validated token. Pre-selection discovery permits only the subject's own active membership and corresponding minimal company label; it cannot enumerate other members. Administrative membership reads/writes additionally require current identity-admin authorization. Provisioning privileges are limited to documented operational commands and never used by normal HTTP requests.

**Acceptance:**

- Given no subject/company context, when the actual runtime role queries company-owned records, then return no unauthorized data and reject writes.
- Given verified subject A with memberships in A/B, when discovering memberships before selection, then expose only that subject's active memberships; forged subject input cannot alter this identity.
- Given trusted company A, when querying/joining/counting/updating B rows or linking a foreign company reference, then expose no B data and reject foreign writes, including INSERT and UPDATE checks.
- Given a pooled connection used for A and then B or an unauthenticated operation, when the prior transaction commits or rolls back, then no A context survives.
- Given revoked membership or reduced role, when a new transaction starts, then old tokens and old selected-company values confer no withdrawn access.
- Given migration/provisioning access exists, when the HTTP application runs, then its credentials cannot assume those privileged roles or bypass policies. Record the FORCE RLS decision for each owned table and verify the runtime role is not an owner.
- Given an operation without a user, including a future job entry point, when no explicit verified service scope is established, then deny rather than default to all companies.

**Tests:** PostgreSQL tests using actual runtime and provisioning roles, not only test-owner connections; HTTP tests for trusted context; sequential pooled reuse, rollback, joins, aggregates, cross-company foreign keys and revocation. **Open decisions:** none for synthetic development after prerequisites. Production backup/restore and operational MFA remain separate gates.

## Delivery and Jira placement plan

Verified order at the top of the OLARIZE backlog:

1. ORG-001 — company provisioning (new).
2. EMP-001 / OLARIZE-1 — existing trusted context, with the explicit ownership/dependency revision above.
3. IAM-001 — membership administration (new).
4. IAM-002 — PostgreSQL isolation (new).
5. Existing EMP-002 / OLARIZE-2 and all remaining employee tickets, preserving their relative order.

EMP-001 is an identity story despite its historical EMP prefix. Moving this existing ticket into the prerequisite group avoids a duplicate. IAM-001/002 are readiness prerequisites before employee record implementation under this proposed order; their position is not a claim that every employee story requires a full identity product.

Publication verified 2026-09-28 through project-local Atlassian MCP: project Olarize / OLARIZE, ID 10033; create permission and Story type 10040 confirmed; board 34 and Rank field 10019 confirmed. All existing issues were searched before creation; no duplicate stable IDs found. Created OLARIZE-18/19/20 and updated OLARIZE-1. Read-back descriptions match prepared content after Markdown bullet/whitespace normalization. Board backlog and `sprint IS EMPTY ORDER BY Rank ASC` both returned OLARIZE-18 → OLARIZE-1 → OLARIZE-19 → OLARIZE-20 → OLARIZE-2, followed by the remaining employee issues in their original order. All four are À faire and outside sprints. Added Blocks links along that sequence through OLARIZE-2. No stories withheld from publication.

## Review decisions and next development handoff

The user approved the three new stories, reuse/revision of EMP-001, their order, controlled company provisioning, and the IAM-001 delegation restrictions on 2026-09-28. Named production authorities and legal/operational release evidence remain pending; no legal calculation or retention policy is approved by this draft.

First independently buildable story: **ORG-001**.

```text
Use $olarize-feature with docs/requirements/organization-identity.md,
story ORG-001. Implement only the approved backend company provisioning
slice, including its controlled command, atomic audit, migrations,
module contract, runbook and PostgreSQL tests. Use synthetic data.
```

This is a future handoff, not an implementation request. No application code changed. The existing employee requirements and OLARIZE-1 were synchronized with the approved dependency refinement.

# Task 014 verified employee account links — OLARIZE-8 / EMP-011

Status: backend implemented, verified and independently reviewed

## User outcome

A dedicated company identity-link administrator explicitly links a verified active membership to one employee, revokes or replaces the link with version checks, and leaves atomic restricted audit. Authenticated own-record resolution returns only the employee ID from the current active link.

## Scope and dependencies

Live Jira OLARIZE-8 matches approved docs/requirements/employee.md EMP-011 and EMP-BR-001–003/006–008/015/018. EMP-001/002 and controlled membership provisioning exist on base a1eb4944c0c1d4d9c0146ad3631b95d26c68f3cb. Backend-only olarize-feature scope; no HR fields, account creation, invitations, email, AI or product frontend. Production verification/provisioning authority remains OD-03. Synthetic development uses existing Keycloak subject-verification adapter and scoped membership IDs, never name/email matching.

## Contract

GET /api/v1/companies/{companyId}/employee-account-links/{membershipId} returns admin-only link metadata (UNLINKED/version0 when absent). POST same path /changes confirms LINK, RELINK or REVOKE with employeeId (absent for revoke), expectedVersion, expectedMembershipVersion, reason, verificationEvidenceReference, idempotencyKey and confirm=true. Requires current identity.link.admin, separate from ordinary HR/employee/manager grants; self-linking is forbidden. GET /api/v1/companies/{companyId}/my-employee-link resolves the authenticated membership's active link and returns minimal IDs only; it grants no HR permission.

Link state uses JPA optimistic version and composite company foreign keys. One active link per membership/company and per employee. RELINK requires an existing LINKED state and a different employee; REVOKE retains the prior employee and audit. Equal actor/company/operation/key retries return immutable original receipt; changed inputs conflict. Keycloak verification runs before database mutation and outside its transaction; a final scoped row lock rechecks actor, target membership state/version. Signed transaction context and restricted RLS enforce writes and own/admin reads. Timestamp precision is microseconds.

Security-definer membership metadata/locking and employee-existence operations are narrow PostgreSQL exceptions needed to validate relationships without granting identity administrators HR-record access; expose identity operations through a public identity port and use JPA for link/audit CRUD. Runtime never owns/bypasses protected tables and cannot update audit/receipts. No general HR-read policy is added for link administrators.

## Acceptance checks

Unit transition, self-link, mandatory evidence/reason and malformed target rules. HTTP/PostgreSQL success and own resolution, denial/foreign targets, membership/provider verification, duplicate/stale/concurrent writes, exact retry, rollback, revoke/relink with old tokens, forged subjects/RLS and audit isolation. Persistent local seed/Swagger calls and operator provisioning/evidence runbook. Full backend verification plus independent findings-only review.

## Verification

Focused verification passed 48 tests; final full ./scripts/verify-backend.sh passed 138 tests, zero failures/errors/skips, and packaged the backend on 2026-10-01. Evidence: 014-verification.md; local generated logs were inspected and removed after review. Independent review identified P2 missing controlled permission provisioning. Added V14 dedicated restricted operator grant/revoke, preserving unrelated permissions with atomic immutable authority audit, version checks and scoped exact retries; authority-focused regression passed 46 tests and focused re-review confirmed resolution with no new actionable findings. Final suite passed 138 tests.

## Completion notes

No frontend or live production-verification claim; requirements approval does not activate production authority or privacy gates.

Persistent local smoke: packaged backend applied V13, V14 and local-only V909 successfully to existing Compose PostgreSQL. Readback confirmed all three success=true and demo membership020 -> employee070 LINKED/version1. Generated local Swagger contains all three paths and the confirmed REVOKE example. Temporary backend processes were stopped. No live authenticated Keycloak/provider or browser-login claim is made.

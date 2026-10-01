# Verified employee account links (EMP-011 / OLARIZE-8)

An active company member with the dedicated `identity.link.admin` permission can explicitly link another verified membership to an employee, replace that link, or revoke it. Ordinary identity administration and HR permissions do not grant this authority. No self-linking, account creation, invitations, email matching or HR permissions are granted by these operations.

## API

All routes require a validated Keycloak access token with `profile.read`.

| Method and route under `/api/v1/companies/{companyId}` | Result |
|---|---|
| GET `/employee-account-links/{membershipId}` | Administrator metadata: IDs, status, version and recorded time; an existing membership without a link returns UNLINKED/version 0. |
| POST `/employee-account-links/{membershipId}/changes` | Confirm LINK, RELINK or REVOKE. |
| GET `/my-employee-link` | Current authenticated member's company and employee IDs only; no HR facts or audit evidence. |

The POST body requires `action`, `expectedVersion`, `expectedMembershipVersion`, `reason`, `verificationEvidenceReference`, UUID `idempotencyKey` and `confirm: true`. LINK/RELINK also require `employeeId`; REVOKE omits it. Inspect the membership and link first and submit their current versions. LINK requires no active link; RELINK requires a different employee and an active link; REVOKE requires an active link. Revoked links can be linked again using their current version.

Before LINK/RELINK, Keycloak subject verification must succeed for the exact stored membership subject. The final transaction locks actor/target memberships and rechecks their current authority, subject, active state and target version. Provider calls run outside the transaction. One active link is allowed per membership and per employee in the company. Optimistic versions and a partial unique index reject races. Link changes and immutable before/after audit commit together, including reason, evidence reference, actor, timestamp, membership/link versions and request correlation. Evidence is an operator reference, never inferred from a name or email.

Same company/actor/link-change key and identical input return the original receipt, including after later changes or a provider outage. Reusing the key with different input conflicts. Current administrator permission is still required for retry access. Every read uses current database state; an old token cannot restore a revoked link or select a replaced employee. Responses use `Cache-Control: no-store`.

Errors follow the platform problem contract: 400 malformed/unknown/unconfirmed input; 401 invalid authentication; 403 missing authority or self-linking; scoped 404 unavailable membership/account/employee or absent own link; 409 stale versions, duplicate active targets, invalid transitions or conflicting retry; 503 unavailable verification provider. Unexpected persistence/audit failures return sanitized 500 and roll back the entire change.

## Local Swagger

Set up the existing synthetic account passwords as described in [authentication](authentication.md). Local-only V909 grants the demo admin explicit link authority and seeds ordinary member `c0000000-0000-4000-8000-000000000020` (Keycloak subject ending `0021`) linked to Yasmine's employee `c0000000-0000-4000-8000-000000000070`. The company is `c0000000-0000-4000-8000-000000000001`. This one-time migration persists in the local database volume and does not reset changes on restart.

1. Sign in as `olarize-demo-admin`; GET the member's link returns LINKED/version 1 on untouched seed data.
2. Sign in as `olarize-demo-employee`; GET `/my-employee-link` returns the seeded employee ID without HR fields.
3. As the admin, POST the provided REVOKE Swagger example, using current link/membership versions. The same employee token then receives 404 for own resolution. This modifies the persistent demo link.
4. To LINK again, configure the Keycloak verification service account using [membership administration](membership-administration.md), use action LINK with employeeId ending `0070`, current revoked link version, current target membership version, and a fresh retry key. Provider configuration is required; absent configuration returns 503. No live provider success is implied by the synthetic HTTP tests.

## Operator boundary

Production authority and verification evidence policy remain governed by OD-03; this backend does not authorize operators by itself. Provision `identity.link.admin` through the dedicated V14 operator function below; the bootstrap CLI grants only `identity.admin`, and ordinary role bundles never grant link authority. Do not add it to every HR role. Restrict evidence/audit access and storage to authorized operators; no public audit endpoint is provided.

V13 follows the existing restricted `olarize_identity_provisioner_owner` SECURITY DEFINER role. When provisioning roles before migrations, the migration transfers its four narrow helpers to that role, grants employee SELECT for a scoped existence check, and grants EXECUTE on the employee/contract read policy predicates required by PostgreSQL policy evaluation. The runtime role receives link CRUD (no DELETE), audit SELECT/INSERT (no UPDATE/DELETE) and helper EXECUTE only. It never inherits the helper owner. If roles are created after migrations, apply the V13 grants/ownership block as the migration operator. Keep the existing identity membership grants/policies and signed scope configured. These helpers expose membership metadata/locks and employee existence only; runtime link administrators gain no general HR read access.

Verification: `./scripts/verify-backend.sh` covers domain rules, signed-token HTTP, restricted PostgreSQL RLS, rollback, provider failure, immutable retries, concurrency, current own resolution and local OpenAPI/seeds.

### Explicit operator grant and revoke

Use a personal restricted database login that belongs to `olarize_identity_provisioner`, following [membership provisioning](identity-provisioning.md). This function supplements the existing bootstrap CLI; there is no new web provisioning route or arbitrary permission input. Verify the exact Keycloak subject, active company membership and reviewed operator evidence before confirming a grant. Use existing membership metadata to obtain its current UUID/version. Execution of the following statement is the explicit operator confirmation.

If roles already exist during V14, migration grants are applied automatically. If roles are configured afterward, run as the migration operator:

```sql
GRANT SELECT, INSERT ON public.identity_employee_link_authority_audit
  TO olarize_identity_provisioner_owner;
ALTER FUNCTION public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid)
  OWNER TO olarize_identity_provisioner_owner;
GRANT EXECUTE ON FUNCTION public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid)
  TO olarize_identity_provisioner;
```

Execute using the restricted personal operator connection, with prepared parameters supplied by the approved operator tool (never credentials in SQL/history):

```sql
SELECT * FROM public.provision_employee_link_authority(
  :companyId, :membershipId, :action, :expectedMembershipVersion,
  :reason, :verificationEvidence, :idempotencyKey, :correlationId);
```

`action` is GRANT or REVOKE. Both require an existing scoped membership and its current version; GRANT requires ACTIVE and the permission absent; REVOKE removes only this permission. Other permissions, status and ordinary role remain intact. The original membership must retain its ordinary baseline permissions. This procedure never creates or reactivates membership. A role update through ordinary membership administration replaces the role permissions and therefore removes a separate link grant; grant it again only after explicit review if still required.

The function rejects runtime, superuser/BYPASSRLS and table-owner/operator-owner inheritance. Operators receive EXECUTE only. Every change increments membership version and atomically appends immutable before/after permission arrays, evidence, reason, operator, time and correlation to `identity_employee_link_authority_audit`. Same operator/company/key and exact inputs return the original result even after a subsequent revoke; changed inputs or stale/foreign targets fail. SQLSTATE 22023 means invalid input; P0002 means stale or conflicting confirmation; 42501 means denied authority. Persistence/audit errors roll back the capability change. Operator tooling must sanitize errors and keep evidence/results access-controlled. Existing tokens see the new or removed capability on their next API request.

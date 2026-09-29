# Company membership administration

IAM-001 lets a company identity administrator list memberships and prepare an exact membership change for explicit confirmation. The API uses only synthetic/test subjects in development; production identity-administrator authority remains gated by OD-03.

## API

- `GET /api/v1/companies/{companyId}/memberships` lists opaque Keycloak subjects, current status, role bundle, explicit permissions and version for the selected company.
- `POST /api/v1/companies/{companyId}/membership-change-previews` accepts `targetSubject`, optional `roleBundle`, and `reason`. Omitting the bundle requests revocation. It returns a signed preview valid for 30 minutes and writes no membership state.
- `POST /api/v1/companies/{companyId}/membership-changes/confirm` accepts the exact `previewToken`, `expectedTargetVersion`, a UUID `idempotencyKey`, and `confirm: true`. The API rechecks actor authorization, preview expiry and target version before changing anything.

Every route also requires a validated Keycloak token with `profile.read`. The selected company ID is only a requested scope; current active `identity.admin` membership is checked from PostgreSQL on every request. The target is an exact opaque Keycloak `sub`, never a username or email. Self-management and changes to any identity administrator are rejected. Identity-administrator grants and revocation stay in the controlled `provision-membership` procedure.

Permission bundles are versioned in code and mapped to fixed permissions:

| Bundle | Permissions |
| --- | --- |
| `HR_ADMIN` | `employee.read`, `employee.write`, `contract.read`, `contract.write`, `employee.import` |
| `MANAGER` | `employee.direct-reports.read` |
| `EMPLOYEE` | `employee.self.read` |
| `PAYROLL_OPERATOR` | `payroll.roster.read` |

These are application grants, not Keycloak roles. HR access does not include payroll. Downstream employee access still requires its own linked-account and company-scope checks. Do not grant permissions by editing token claims.

## Required runtime setup

The web database login must be a dedicated non-superuser, non-owner and non-`BYPASSRLS` role. It receives membership SELECT only for its own subject and EXECUTE on the membership-administration functions; it gets no direct membership or audit write grant. Follow the owner/runtime separation in [identity provisioning](identity-provisioning.md). The migration's security-definer functions must be owned by the restricted `olarize_identity_provisioner_owner` role, which receives only the table privileges those functions need. Never grant the web runtime membership in that owner or the initial-provisioning operator group.

Grant the runtime role function execution after migrations:

```sql
GRANT EXECUTE ON FUNCTION public.list_company_identity_memberships(text, uuid) TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.change_company_membership(text, uuid, text, text, text, bigint, bigint, text, uuid, text) TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.find_company_membership_change_retry(text, uuid, text, text, text, bigint, bigint, text, uuid) TO olarize_runtime;
```

After V5 is applied, the restricted owner also needs `SELECT` on `organization_company`, `SELECT, INSERT, UPDATE` on `identity_membership`, and `SELECT, INSERT` on `identity_membership_change_audit`. Transfer ownership of all three V5 functions and the new audit table to `olarize_identity_provisioner_owner`; preserve the separate migrator ownership of the existing membership tables/functions. See the integration fixture in `CompanyContextIntegrationTests` for the exact owner and execution grants used by tests.

Configure the Keycloak service account and shared preview key through the secret manager/environment:

| Variable | Purpose |
| --- | --- |
| `KEYCLOAK_ADMIN_BASE_URL` | Private Keycloak base URL reachable by the backend, such as `http://keycloak:8080` inside local Compose. |
| `KEYCLOAK_REALM` | Realm containing the verified users; defaults to `olarize`. |
| `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET` | Service-account client credentials with permission to query users by immutable ID. |
| `IDENTITY_PREVIEW_SECRET` | Shared signing secret of at least 32 bytes. Use the same value on every backend replica. |

The adapter exchanges the client credentials for a short-lived access token and asks Keycloak for the exact user ID. Missing configuration/provider outage blocks new grants and reactivations; revocation remains available. Do not log tokens, credentials or subject lookup responses. Rotating the preview signing secret invalidates pending previews. Keep credentials out of `.env.example`, source control, request bodies and logs.

## Change safety

The preview binds actor and target versions, company, subject, action, role bundle, permission set and reason. Confirmation checks them again under transaction locks. Membership mutation, version increment and immutable before/after audit record commit together. An exact retry first checks the committed audit receipt; it returns the original result without depending on a second Keycloak call. Different input with a used key conflicts. Permission reduction and revocation are read from current database state and affect the user's next request even if its access token remains valid.

Revoked ordinary memberships retain their ID and history and can be reactivated after Keycloak confirms the subject still exists. No invitation, password reset, employee/account link or public self-enrollment is provided. See [API contract](api/openapi.yaml) for request/response and HTTP error details.

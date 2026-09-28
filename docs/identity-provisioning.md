# Controlled membership provisioning

This runbook covers synthetic/local setup for EMP-001's initial identity administrator. It does not authorize customer onboarding. A named production operator, verified Keycloak account evidence, approved hosting/data controls and backup/recovery gates are still required.

## Roles and grants

Migrations do not create login credentials. On a new database, a database administrator creates separate NOLOGIN owner and operator-group roles, then grants the new function and restricted table rights. Keep the normal web runtime separate.

```sql
CREATE ROLE olarize_identity_provisioner_owner NOLOGIN NOSUPERUSER NOBYPASSRLS;
CREATE ROLE olarize_identity_provisioner NOLOGIN NOSUPERUSER NOBYPASSRLS;
CREATE ROLE olarize_identity_migrator NOLOGIN NOSUPERUSER NOBYPASSRLS;

-- Run as the database migration/schema owner after V4 is applied.
GRANT USAGE ON SCHEMA public TO olarize_identity_provisioner_owner, olarize_identity_provisioner;
GRANT CREATE ON SCHEMA public TO olarize_identity_provisioner_owner;
GRANT SELECT ON public.organization_company TO olarize_identity_provisioner_owner;
GRANT SELECT, INSERT, UPDATE ON public.identity_membership TO olarize_identity_provisioner_owner;
GRANT SELECT, INSERT ON public.identity_membership_audit TO olarize_identity_provisioner_owner;
ALTER TABLE public.identity_membership OWNER TO olarize_identity_provisioner_owner;
ALTER TABLE public.identity_membership_audit OWNER TO olarize_identity_provisioner_owner;
ALTER FUNCTION public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid)
  OWNER TO olarize_identity_provisioner_owner;
REVOKE CREATE ON SCHEMA public FROM olarize_identity_provisioner_owner;
GRANT EXECUTE ON FUNCTION public.provision_identity_membership(uuid,text,text,bigint,text,text,uuid,uuid)
  TO olarize_identity_provisioner;
GRANT olarize_identity_provisioner TO olarize_operator_alice;

-- Runtime can read only its company-scoped membership rows. It cannot execute provisioning.
GRANT USAGE ON SCHEMA public TO olarize_runtime;
GRANT SELECT ON public.identity_membership, public.organization_company TO olarize_runtime;
```

Use a personal login with MFA through the approved database access process. Do not make operators, the group, owner or runtime role superusers or BYPASSRLS. Do not grant operators membership in the owner role. The owner must remain NOLOGIN. Apply role grants through controlled infrastructure/DBA change management, not application startup.

## Command

Set only `IDENTITY_PROVISION_DB_URL`, `IDENTITY_PROVISION_DB_USERNAME`, and `IDENTITY_PROVISION_DB_PASSWORD` in the operator's secret manager/session. Never put them in `.env`, command-line arguments or JSON. Then run:

```sh
java -jar backend/target/olarize-backend.jar provision-membership < request.json
```

The command consumes at most 16 KiB of JSON on stdin. The exact fields are `companyId`, `subject`, `action`, `expectedVersion`, `reason`, `evidence`, `idempotencyKey`, and `confirm`. Confirmation must be the JSON boolean `true`. `subject` is the exact immutable Keycloak `sub`; `evidence` records the operator's prior verification of that account in the configured realm. Neither is inferred from a name or email. Do not include tokens, credentials, employee records or other customer data in the JSON. The verified-subject evidence is a controlled operator assertion, not a live Keycloak lookup.

For first grant use `action: "GRANT"` and `expectedVersion: 0`. It grants only `identity.admin`; permissions cannot be supplied by the caller. For revocation use `action: "REVOKE"` and the current membership version returned by the prior operation. Re-read/verify membership state before an intentional new operation. Use a new UUID idempotency key for each new confirmation; retry the exact same request/key after an uncertain result. A changed payload with a used key, stale version, duplicate active grant or repeated revoke fails without changing data. There is no normal-operator grant for other roles; IAM-001 owns that future workflow.

The database function authenticates the actor as `session_user`, serializes operations by operator/key and company/subject, checks current company existence, writes the membership and immutable audit atomically, and runs as a NOLOGIN restricted role under FORCE RLS. Operators receive function EXECUTE only and no direct table writes. Membership reads use the authenticated request subject and transaction-scoped `olarize.subject`; no application route accepts a target subject. Each request rechecks current membership and permission state, so revocation or permission reduction affects a still-valid token on its next call.

Exit codes: 0 success/retry; 2 invalid input; 3 stale, duplicate or conflicting operation; 4 denied credentials/authorization; 5 sanitized persistence failure. JSON errors include a request ID and omit SQL, password, subject, reason and evidence. Keep the output as an access-controlled operational record.

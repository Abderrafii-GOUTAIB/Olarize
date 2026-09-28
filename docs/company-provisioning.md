# Provision a company — ORG-001

This backend command registers a company and its immutable creation/audit receipt in one transaction. It does not provision memberships, expose an HTTP route or start the web server. Use synthetic companies until the production grant authority and other customer-data gates are reviewed.

## One-time database administration

Apply Flyway migrations with the separate migration account first. A database administrator then creates a non-login provisioning group and individual operator logins. These are operational role grants, not migrations or web startup actions. Run the following once, adapting the synthetic login name; do not run it with web credentials:

```sql
CREATE ROLE olarize_company_provisioner NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
CREATE ROLE company_operator_example LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
GRANT olarize_company_provisioner TO company_operator_example;
GRANT USAGE ON SCHEMA public TO olarize_company_provisioner;
GRANT SELECT ON public.organization_company, public.organization_company_creation
    TO olarize_company_provisioner;
GRANT EXECUTE ON FUNCTION public.provision_company(uuid, text, text, uuid, uuid)
    TO olarize_company_provisioner;
```

Set the personal login's password through the database administrator's secure credential workflow (for example psql's interactive `\password company_operator_example`), or configure reviewed database certificate authentication. Do not put passwords into SQL files, source, shell history or Jira. Credentials must identify one operator; a shared credential would lose individual attribution. Grant database CONNECT if the environment has revoked its default grant. Operator logins must not own these tables, belong to the migration/owner role, be superusers or bypass RLS. Operators receive SELECT on the tables and EXECUTE on the provisioning function; they receive no direct INSERT, UPDATE or DELETE. The database function records `session_user` and commits company creation plus its audit receipt atomically. Do not grant provisioning-role membership to the web runtime.

The initial local Compose database credential is a development bootstrap credential; it is rejected by this command because it is privileged. Shared runtime credentials must remain separate from migrations and provisioning. Company tables have FORCE ROW LEVEL SECURITY, and no PUBLIC privileges. Future EMP-001 runtime lookup integration needs SELECT on `organization_company`, a verified membership authorization provider and a restricted runtime role; do not grant that role INSERT on either table. The current application exposes no company read route and denies lookup without the provider.

## Execute one confirmed creation

Build and verify using `./scripts/verify-backend.sh`. Inject these variables through your approved secret manager or a private process environment:

- `ORG_PROVISION_DB_URL`: JDBC PostgreSQL URL; use the reviewed TLS configuration for shared environments.
- `ORG_PROVISION_DB_USERNAME`: individual provisioning login.
- `ORG_PROVISION_DB_PASSWORD`: its secret.

The command never falls back to `DB_*`, `.env` or Spring web datasource settings. Credentials are not accepted in JSON or command arguments. Invoke from the repository root with a reviewed synthetic input:

```sh
java -jar backend/target/olarize-backend.jar provision-company <<'JSON'
{"name":"شركة Démo","reason":"Synthetic company setup","idempotencyKey":"11111111-1111-4111-8111-111111111111","confirm":true}
JSON
```

Use a fresh UUID for a new operation. Review the input before executing; `confirm: false` is rejected without connecting or writing. Input is UTF-8 JSON limited to 16 KiB. Only the four fields shown are accepted; duplicate keys and trailing content are rejected. Name and reason are trimmed, nonblank Unicode, limited to 200 and 1,000 code points. Names are not unique legal identifiers. No registration number, employee record or legal-compliance verdict is created.

Success returns only `companyId`, `version` (1) and `requestId` on stdout. The company name is not printed. Errors return a sanitized code and request ID on stderr, without exceptions, SQL or input values:

| Exit | Code | Meaning |
| --- | --- | --- |
| 0 | Success JSON | Committed creation or identical retry |
| 2 | ORG_INVALID_INPUT | Invalid/unconfirmed input or missing command configuration |
| 3 | ORG_CONFLICT | Same operator/key used with different normalized name or reason |
| 4 | ORG_DENIED | Database session is not an eligible provisioning operator |
| 5 | ORG_FAILED | Connection/persistence or unexpected failure; no success claimed |

If a connection breaks after commit, retry with the same operator, key and payload to resolve the outcome. Do not generate a new key merely because the first response was lost. Concurrent identical requests serialize and return the same company. Identical names with different operation keys intentionally create distinct company identities.

Audit contains the authenticated database `session_user`, company ID, action `COMPANY_CREATED`, reason, recorded time, operation key and original creation correlation ID. A retry has its own response request ID but never overwrites original audit. Audit and company creation either both commit or both roll back. There is no delete/rename/correction operation in this story. Audit visibility through operator credentials is limited to that operator's own creation receipts; wider review requires a separately authorized operational process.

The provisioning role cannot insert directly into either table. The command invokes `public.provision_company(...)`, a fixed-search-path security-definer function owned by the migration/table owner. The migration revokes its default PUBLIC execute grant and grants execution to the existing provisioning group when present. If the group is created after migration, the administrator grants function execution as shown above. The function validates inputs, serializes same-key requests, derives the actor from the database session, and writes the company and audit receipt atomically.

## Access revocation and follow-up

A database administrator can revoke the group membership and disable the personal login through the approved operational process. Stop any existing operator sessions as part of urgent revocation. Never treat shell access or a Keycloak administrator role as automatic provisioning authority. EMP-001 defines application memberships and initial identity-administrator setup; IAM-001 adds membership management; IAM-002 extends verified context and policies across those tables. No production provisioning approval is implied by passing synthetic tests.

## Module contract

`organization::api` exposes `CompanyLookup.find(companyId)` and immutable `CompanyIdentity`. It checks a unique `CompanyReadAuthorization` provider before any company query. Missing or ambiguous providers deny. EMP-001 will implement this SPI using current authenticated membership; neither a UUID nor a request header is trusted context. Each lookup runs in a separate read-only transaction, sets transaction-local database scope and applies an explicit company predicate. An unauthorized target returns an empty result without exposing its existence. The contract is internal to the modular backend; it adds no OpenAPI endpoint.

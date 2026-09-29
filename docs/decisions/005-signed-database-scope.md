# ADR 005 — Signed PostgreSQL request scope

Date: 2026-09-29
Status: accepted for IAM-002 and current identity/organization tables.

## Problem

PostgreSQL row-level security used transaction-local `olarize.subject` and `olarize.company_id` settings. A caller with the HTTP runtime database credential could set those values to another user and company. The runtime role could also call existing security-definer membership functions with an administrator's user ID. Application token checks protected normal HTTP requests, but the database could not distinguish verified context from forged SQL input.

## Decision

The application validates the Keycloak access token and signs the subject, optional company ID and short expiry for each database transaction. PostgreSQL verifies the HMAC proof using a key in a private schema before accepting RLS subject scope. Guarded membership-administration functions require the same verified subject and company as their actor arguments. The runtime role cannot execute the renamed internal functions or read the key table. The signing key is separate from the runtime database password and is supplied to the one-off migration job and backend replicas through secrets.

Keep the existing focused JDBC operations for transaction-local scope and atomic security-definer membership changes. They are PostgreSQL-specific exceptions to the JPA default in ADR 004. Provisioning roles retain their documented policies; runtime roles cannot assume them. A future company-owned table must add its own forced RLS policy and composite company constraints.

## Alternatives considered

Unsigned custom PostgreSQL settings would leave runtime-credential impersonation open. A separate database login for each Keycloak user would complicate connection pooling and role lifecycle. Verifying Keycloak JWT signatures inside PostgreSQL would duplicate the resource server's issuer/key handling. The signed transaction scope keeps token validation in Spring while giving PostgreSQL a separate proof that the runtime login alone cannot forge. This extends ADR 003's tenancy direction and does not supersede ADR 004.

## Consequences and verification

Losing only the runtime database credential cannot forge another user's scope or call an unguarded administrator function. Losing both the credential and signing key remains outside this boundary. Key rotation requires a coordinated database and backend rollout. Missing or malformed key configuration stops the backend at startup; a mismatched key denies protected reads. Local Flyway permits out-of-order shared migrations after its V900/V901 demo migrations, preserving the persistent sample data.

Restricted-role PostgreSQL tests cover missing and forged settings, company tampering, direct administrator-function impersonation, revocation, pooled transaction cleanup, privilege grants and same-company audit references. Signed-token HTTP tests cover the working API. The full backend suite passed 79 tests locally, and an existing local demo database upgraded to V6 while retaining its company and memberships. Production key delivery, owner-role setup and rotation still require deployment verification.

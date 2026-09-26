# ADR 003 — MVP hosting and customer-data protection

Date: 2026-09-24
Status: accepted direction; provider selection, implementation and production verification pending.

## Problem

HR customers need a precise account of where employee records, documents, identity data, logs and backups reside, who can access them, and whether any external AI/development provider receives them. The previous AWS examples and early-AI roadmap did not settle these questions.

## Decision

Target managed hosting in Morocco for the MVP. No specific provider, data centre, contract or production deployment has been selected or verified. Verify primary storage, separate backups, logs, identity services, support access and all subprocessors before making a residency promise. Record outbound email, monitoring and other integrations too; a Moroccan application server alone does not establish that all processing stays in Morocco.

Keep the portable Spring Boot modular monolith. Use a shared private PostgreSQL business database with trusted company membership, application authorization and PostgreSQL row-level security as an additional isolation layer. Company-owned rows and references must preserve company ownership. Use a restricted application role separate from migrations and backups; test policies with the actual runtime role, including missing context, pooled connections and background jobs. Shared infrastructure is an MVP tradeoff, not physical separation between customers. Dedicated customer databases may be evaluated later for concrete requirements.

Use private object storage with company-scoped access; the provider and storage implementation remain to be selected. Keycloak keeps a separate database. Enable TLS, encryption at rest, restricted operational access and administrator MFA. Backups must be encrypted, separate from the running server, have verified Moroccan storage locations and a tested recovery procedure. Define recovery objectives, data-class retention, customer exports, deletion and backup expiry; do not invent fixed retention periods or promise immediate deletion from every backup.

The initial MVP does not send employee records, personnel documents, payroll data or confidential company information to external AI/model/embedding services. Product AI and RAG are deferred for the current implementation phase; future activation needs a separate scoped review of the actual data, provider, countries, retention and applicable customer/legal requirements. No policy-only exception is assumed: policies may also contain confidential or personal information.

Development agents, the workbench, Jira, GitHub and ordinary tests use synthetic data only. Do not give them production credentials, production database connections, real employee documents or database dumps. Development/test and production environments remain separate. Codex login permission is a distinct tooling decision; this ADR does not authorize access outside the project's local-folder boundary.

## Before onboarding real customer data

- Select a provider and document storage/processing locations, backup locations, subprocessors and support access in writing.
- Implement and verify company isolation for reads/writes, relationships, document downloads, exports and jobs, including database policy bypass and connection reuse cases.
- Restrict administrator/support access, verify safe logs and document incident response responsibilities.
- Complete a backup restoration exercise and agree recovery/retention/exit procedures.
- Review customer data-processing terms, responsibilities and applicable CNDP formalities with qualified Moroccan data-protection advice. Hosting in Morocco does not by itself establish compliance. Foreign access or processing must be assessed separately.

These are production-onboarding gates. Synthetic-data feature development can continue while provider/legal decisions are resolved.

## Alternatives considered

- Foreign cloud / original AWS option: remains a possible later alternative, subject to an explicit decision about requirements, transfers, operations and costs; not the current MVP default.
- Database per customer: stronger operational separation but more migration/backup/provisioning work; defer unless a concrete customer or risk requirement justifies it.
- External AI over HR records immediately: deferred to reduce initial data exposure and compliance scope while deterministic HR services are built.

## Consequences and precedence

This decision supersedes earlier AWS-default and early-product-AI sequencing assumptions in project guidance for the current MVP. It does not erase the longer-term Business V1 vision, rewrite the preserved business source or change approved Jira stories automatically. Stories containing AI/UI scope must be explicitly deferred or split before backend-only implementation; do not report partial work as full story completion.

The security contract is `.instructions/reference/security.md`; implementation guidance stays in the existing backend and infrastructure files. No new skill, agent or hook is needed for this decision.

## Verification evidence

Documentation aligned with the user's accepted MVP direction. No infrastructure was provisioned, no database policies were deployed, no customer data was processed, and no CNDP filing or legal certification is claimed.

Sources checked during the decision discussion:
- https://www.cndp.ma/procedures-de-notification-process/
- https://www.cndp.ma/transfert-de-donnees-a-letranger/
- https://www.postgresql.org/docs/17/ddl-rowsecurity.html

Reverify the applicable legal procedure and provider commitments before production onboarding.

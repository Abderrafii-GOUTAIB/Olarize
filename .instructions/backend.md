# Spring Boot backend instructions

Use Maven with its wrapper, a compatible Java LTS toolchain, Spring Boot, Spring Security, Spring Modulith, Spring Data JPA, Bean Validation and Flyway. Verify Spring AI compatibility during foundation work and integrate it in the first authenticated AI slice. Use deterministic provider stubs in ordinary tests. Select and pin a compatible release set from official documentation; do not guess latest versions.

## Module structure

Use package-by-business-module under com.olarize: identity, organization, employment, attendance, leave, payroll, socialprotection, documents, knowledge, assistant, notifications and audit. Create module implementations only as required by the active slice. Keep the application entry point above these packages.

Expose deliberate public module APIs. Put implementation details in internal packages. Do not call another module's repository or import its internal entities. Use identifiers and module APIs rather than cross-module JPA object graphs. Keep shared types small; do not create a generic shared-service dumping ground. Add Spring Modulith verification for prohibited dependencies and cycles.

Controllers validate transport input and call application services. Application services establish trusted context and transaction boundaries. Domain logic implements explicit rules. Infrastructure adapters handle persistence and providers. Prefer clear code over layers containing only pass-through methods.

## API and persistence

- Use request/response DTOs, not JPA entities. Document contracts through OpenAPI when endpoints appear.
- Use UUID identifiers, explicit counting units and BigDecimal where fractional values or money are needed.
- Use LocalDate for leave dates; store instants in UTC for events; use Africa/Casablanca for relevant calendar interpretation.
- Enforce tenant predicates in database access. Verify ownership for every identifier supplied by a client.
- Use a balance ledger with adjustment reasons and rule snapshots, not a mutable balance edited without history.
- Serialize or lock conflicting balance/approval changes within a transaction. Test real concurrent conflicts.
- Make retriable writes idempotent, scoped by company/user/operation; a reused key with different input must not silently succeed.
- Persist business mutation, audit event and outbox intent atomically where they must remain consistent.
- Do not send email or call an LLM while holding a database transaction open.
- Flyway migrations are append-only after application in shared environments. Prefer expand/contract changes over destructive rollout.
- Return consistent problem responses without exposing internals or sensitive data.
- For payroll preparation, maintain versioned input snapshots, periods, exceptions and export/reconciliation states. Do not build a proprietary gross-to-net engine.
- CNSS and CIMR are separate scheme adapters with reviewed applicability, effective-dated rules, bases, ceilings, rounding and employee/employer portions. Missing configuration blocks an authoritative result.
- Employer-cost figures must name their included components, period, sources and whether they are estimates or reconciled values. Missing data is unknown, not zero.
- Support dry-run CSV imports for employee, attendance and payroll inputs: validate mapping, company scope, duplicates and errors before confirmed persistence. This is the initial bridge from fragmented tools.

## Background jobs and tests

Start with durable PostgreSQL jobs and outbox records. Use safe claiming, bounded retries, backoff and visible terminal failures. Assume at-least-once execution and make effects idempotent. In-process timers alone are insufficient for durable work.

Use JUnit and Spring tests, Testcontainers PostgreSQL for database-sensitive behavior, and Modulith verification. Prefer PostgreSQL to H2 for pgvector, locking and tenant-query tests. Cover valid paths, unauthorized access, cross-company identifiers, duplicate requests and rule boundaries. Keep model calls stubbed in ordinary CI; run approved live integration checks separately.

## Clean architecture and errors

Follow the package layout in docs/architecture.md. Group code by business module, then separate its public API and internal domain, application and adapters. Domain code has no Spring, HTTP, JPA or AI dependencies. Application use cases depend on domain and explicit ports; adapters implement persistence/provider ports. Controllers map inputs/outputs. Add abstractions only where they protect a real boundary.

Use the platform HTTP problem contract for controller and security failures. Domain failures map to HTTP only at the web adapter. Never expose exception messages, SQL, secrets or rejected values to clients. Keep request IDs through errors and clear logging context after each request. Shared platform infrastructure must not become a shared business-service layer. See docs/errors-and-debugging.md.

## SOLID review and exception design

Apply the SOLID and purposeful-pattern rules in .instructions/project.md to backend changes. Keep Spring wiring at application/adapter boundaries; domain objects remain framework-independent. Prefer focused ports to generic service/repository base classes. Patterns must not bypass tenant checks, authorization, audit or transactional rules.

Introduce typed domain failures as real business cases appear. Keep any exception hierarchy shallow, module-owned and independent of HTTP. Map expected failures explicitly at the web boundary; unexpected failures use the sanitized global fallback. Security-filter failures use security handlers with the same problem contract. Do not create a universal exception superclass or translate every failure to HTTP 500. Test each implemented mapping and its sensitive-data redaction.

## API documentation and operational logs

Document new HTTP endpoints in generated OpenAPI with accurate authentication, scopes, response types and errors. Keep Swagger UI local only; nonlocal profiles must disable and deny documentation routes unless a reviewed access policy explicitly changes this. Use the existing Springdoc 3.x integration with Spring Boot 4. Avoid describing unimplemented HR endpoints as working. If the checked-in reference YAML changes, keep it aligned with the generated contract.

Use SLF4J and request IDs for operational diagnostics. Docker emits structured JSON to stdout; local host runs stay readable. Never log HR payloads, URLs with query data, bearer tokens, passwords or exception messages that may contain private data. Use bounded, low-cardinality fields for request completion. Keep audit events separate from diagnostic logs. Do not add logging frameworks or file appenders without a concrete need.

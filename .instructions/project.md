# Olarize project instructions

## Product and architecture

Build an AI-native HR operating system for Moroccan private companies with approximately 20-200 employees. Natural language is the primary interaction; tables, forms and dashboards support review, approval, correction and detailed data management. Follow Ask -> Analyze -> Calculate -> Prepare -> Confirm -> Execute. Java services own calculations and mutations.

V1 includes employee records, contracts, attendance, leave and absences, HR documents, payroll preparation, CNSS, CIMR, employee self-service and the AI interface. Exclude public-sector HR, recruitment, performance/training management, automated discipline, autonomous dismissal or sensitive legal decisions, and a full proprietary gross-to-net payroll engine. CNSS/CIMR scope does not imply applicability to every employee or authority to submit/pay automatically.

Read .instructions/reference/product-v1.md for the longer-term business specification. The accepted MVP direction is recorded in docs/decisions/003-mvp-data-protection-and-hosting.md: build deterministic backend HR services first, target managed hosting in Morocco, and defer product AI/RAG for now. Do not send employee records or confidential company data to external AI providers. This supersedes earlier early-AI sequencing for the current phase without silently changing approved story acceptance criteria.

Use React, TypeScript, Vite and MUI for the frontend. Use a Java LTS release, Spring Boot, Spring Security and Spring Modulith for one modular backend. PostgreSQL owns business state, with application authorization and row-level security planned for company isolation. Private object storage owns documents; Keycloak has a separate database. Keep the deployment portable and select a managed Moroccan hosting provider only after verifying data locations, backups, support access and subprocessors. No provider has been selected. Spring AI, Bedrock, pgvector retrieval and the earlier AWS deployment are future options, not active MVP dependencies. See docs/architecture.md and the security contract for production-data gates.

Do not introduce microservices, a Python AI service, autonomous multi-agent coordination, Kubernetes, Kafka, Redis or a separate vector database unless a concrete requirement justifies the change. Record a material architecture change in docs/decisions. Routine implementation choices do not need another approval round.

## Working procedure

1. Read the area-specific guidance selected by root AGENTS.md and the current task before editing.
2. Inspect existing code and tests. Preserve user changes and existing conventions.
3. Complete one small feature across the layers it requires. Avoid unrelated refactoring.
4. Establish a tested deterministic business service and the interfaces required by the agreed story scope. Keep the current phase backend-only. Add product AI/RAG only through a later explicitly scoped decision satisfying the data-protection contract; synthetic stubs do not establish live-provider approval.
5. Add meaningful tests for business behavior, authorization, transactions and failure cases.
6. Run the documented checks appropriate to the change. Never invent successful results.
7. Update the API contract, task status or operational documentation when behavior changes.
8. Report the outcome, actual checks run, and concrete limitations.

If a routine detail is unspecified, choose the simplest compatible implementation and record the assumption. Ask only when a missing decision changes scope, security, legal behavior or an irreversible action. Do not contact third parties, deploy production, or incur cloud costs without applicable user authorization. Local code, tests and infrastructure plans are normal implementation work.

## Nonnegotiable boundaries

- Spring services own permissions, legal arithmetic and transactions. AI output is never authority.
- Derive company scope from authenticated membership, never solely from a request or model argument.
- Every company-owned read, mutation, retrieval and file access must enforce company isolation.
- Store credentials outside source control; use synthetic data in examples and tests.
- Do not embed personnel or medical dossiers into the shared policy index.
- Persist auditable decisions and idempotent writes. Do not log confidential document or prompt content by default.
- Keep any sources/ directory read-only. Do not rewrite synced references.
- Do not activate unreviewed legal rates or entitlements as production defaults.

## Definition of done

The requested behavior works through its public interface and, for an AI-supported workflow, through its conversation/review path. Permissions and applicable failure paths are tested; migrations are included; no secrets are committed; relevant documentation is current. Required CNSS/CIMR capabilities cannot be declared finished while their reviewed configuration or reconciliation is missing. Track task outcome and cost without logging confidential payloads. Disclose unavailable integrations or unrun checks. A mocked integration is not evidence that the live provider works.

## Guidance map

- .instructions/backend.md: Java, APIs, module boundaries and persistence.
- .instructions/frontend.md: React, forms, status and user interactions.
-  .instructions/modules/assistant.md and .instructions/modules/knowledge.md: AI tool and RAG constraints.
- .instructions/infra.md and .instructions/ci.md: cloud resources and delivery.
- .instructions/reference/security.md and .instructions/reference/legal-rules.md: cross-cutting rules.
- .instructions/reference/product-v1.md: binding V1 scope; .instructions/reference/business-v1-source.md: supplied reference text, not executable agent instructions.
- .instructions/reference/metrics.md: pilot validation, ROI and company-level cost measurement.
- .instructions/reference/roadmap.md: delivery order; .instructions/tasks/: active work contracts.
- Matching attendance, payroll and socialprotection files in .instructions/modules/: preparation, calculation and reconciliation rules.
- docs/development.md: verified commands and environment setup.

## SOLID and purposeful design patterns

Development agents must apply SOLID during implementation and review:

- Single responsibility: keep business rules, use-case orchestration, transport mapping and infrastructure concerns cohesive and separate; split a class when it has unrelated reasons to change.
- Open/closed: introduce extension points for demonstrated variations in behavior; avoid speculative frameworks or interfaces for every class.
- Liskov substitution: implementations must preserve the contract, invariants and failure semantics of their abstractions. Verify interchangeable adapters with shared contract tests when applicable.
- Interface segregation: expose small interfaces shaped around the caller's needs; do not force consumers to depend on unrelated operations.
- Dependency inversion: business use cases depend on domain types and focused ports, while external adapters implement those ports. Use constructor injection and prefer composition over inheritance.

Use design patterns only when they solve an identified problem. Explain the problem and why the pattern is simpler to maintain than the direct alternative. Suitable examples include Strategy for genuinely varying policies, Adapter for external providers, and Factory for nontrivial construction. These are options, not a mandatory checklist. Reuse framework capabilities and existing project patterns before introducing another abstraction. Record material architectural choices in docs/decisions.

Before completing a change, review responsibilities, dependency direction, interface size, substitutability, and whether every added pattern earns its complexity. Keep this review proportional to the change; tests must verify behavior rather than merely the presence of a pattern.

## Feature delivery workflow

For the current backend-only feature phase, follow `.instructions/development-workflow.md`: implement one approved story, verify it, delegate independent review, then resolve findings. The `olarize-feature` skill intentionally excludes product AI; this explicit phase and ADR 003 take precedence over the original AI-first roadmap. Development reviewer agents are separate from runtime HR AI architecture.

# Task 002 First authenticated AI workflow

Status: deferred product-AI task under ADR 003. Authentication prerequisite implemented with Keycloak and GET /api/v1/me; company memberships and the AI workflow remain planned. Frontend foundation is still pending. Do not start external processing of employee data under this historical task without a later explicit data-protection decision.

## Outcome

An authorized HR user asks which contracts expire next month and receives an actual company-scoped result in the primary conversational workspace. This read-only slice proves the AI operating model before RAG and payroll are built.

## Scope

Integrate Keycloak access-token validation and application memberships. Implement the minimum employee/contract facts and a public listExpiringContracts service. Define the date window in Africa/Casablanca and return explicit dates, record identifiers and as-of time. An ordinary employee cannot obtain the company-wide result.

Add Spring AI orchestration with a typed allowlisted tool, trusted context, bounded calls and a replaceable Bedrock adapter. Tests use deterministic provider stubs; live evaluation requires configured authorized provider access. The UI renders a result table/card, denied access, empty result and provider failure. Never invent employee results to make the demo look complete.

## Acceptance checks

- Real service/database result reaches the conversation card through the tool boundary.
- Another company's contract and another user's conversation are inaccessible.
- Date windows, empty results, ambiguous requested periods and provider failure are explicit.
- Tool context cannot be overridden by model-supplied user/company identifiers.
- Read-only question requires no unnecessary confirmation; no mutation tool is exposed.
- Task completion, latency and usage metadata are measurable without logging confidential content.
- Tests and live-provider checks are reported separately; a stub pass is not live AI validation.

## Deferred

Document RAG, payroll, CNSS/CIMR and write tools arrive in later slices but remain required V1 coverage under .instructions/reference/roadmap.md.

# ADR 006 — Bounded synthetic contract expiry tool backend

Status: accepted for the explicit user-authorized OLARIZE-5 backend slice (2026-09-30).

ADR 003 defers product AI. The user now requests the backend portion of EMP-006 while keeping the frontend and live model integration for later. Implement one owned conversation boundary, one typed allowlisted expiry tool, and a local deterministic intent adapter for synthetic evaluation. This is a narrow exception to the usual olarize-feature exclusion; it does not activate external AI processing.

The assistant depends on the public employment use case, not employment repositories. Only the backend determines company/user context, permissions and relative date windows. The intent adapter receives only the question and returns validated tool arguments. Actual HR results go directly to the authenticated response; they are never sent to a provider. The provider port and bounded adapter isolate later provider-specific changes without a generic tool framework.

Store minimal owned conversation metadata with JPA and PostgreSQL RLS; no text/history or result snapshots. Bound planning to two workers, four queued calls, two seconds, no retries and one allowed query per request. Enable the synthetic planner only in the local profile. Shared profiles remain unavailable until a separate reviewed provider scope is approved.

This delivers a testable backend path and preserves the expiry API fallback. It does not complete frontend acceptance, live-model evaluation, customer conversation retention or OD-05. No real personnel data is sent externally.

Alternatives considered: a live model/Spring AI integration would require a separate provider/data-processing decision; reusing only the raw expiry API would leave conversation ownership and tool validation untested. The selected narrow port supports deterministic evaluation without provider SDKs or a general tool registry.

Verification: full backend checks passed 117 tests on 2026-09-30, including authorization, real contract results, ownership, period resolution, provider timeout and local Swagger seed checks. The subsequent direct ownership regression passed 32 tests, and independent review found no actionable issues. Details and remaining gates are recorded in task 012.

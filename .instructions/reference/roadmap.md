# Business V1 ranked delivery plan

Current-phase override: ADR 003 targets managed Moroccan hosting and deterministic backend development first; product AI/RAG is deferred for now and external AI processing of real employee/confidential company data is excluded from the initial MVP. This document preserves the longer-term vision and original sequence; it does not override that later accepted decision.

Product validation and development inform one another. Interviews and pilot targets are tracked separately; they are not authorization for an agent to contact anyone. Build the primary conversational experience early, after its service and permission boundaries exist.

| Rank | Delivery slice | Completion evidence |
|---|---|---|
| 1 | Confirm five business examples, roles, pilot data sources, outcome baselines and V1 acceptance | Scenario contracts and assumptions; validation targets recorded, not claimed achieved |
| 2 | Backend/React conversational shell, local PostgreSQL, Flyway, CI and compatible versions | Reproducible builds and honest connection status; task 001 |
| 3 | Keycloak, company isolation, employee/contract facts, first bounded AI tool for expiring contracts | Actual authorized service result in conversation; duplicate names, date windows and cross-company tests; task 002 |
| 4 | Leave plus attendance records/schedules/corrections and absent-today tool | Approved absence differs from missing clock-in; reviewed ledger rules and concurrent decisions pass |
| 5 | Early AWS staging and tenant-level telemetry | Thin AI-first workflow works over HTTPS; task outcomes, provider latency and cost visible |
| 6 | Documents, approval, ingestion and policy RAG | Cited answers, lifecycle/revocation checks and reviewed French/Arabic cases |
| 7 | Payroll preparation period and variable assembly with conversational preview | Authorized roster, period, source snapshots, exceptions and reconciliation-ready output; no invented net pay |
| 8 | CNSS and CIMR applicability, reviewed calculation/configuration and employer-cost query | Independent expected totals, employee/employer breakdown, missing-input blocking and explicit estimate labels |
| 9 | Confirmed payroll/HR actions, controlled exports and declaration/payment evidence | Exact batch confirmation, stale-preview checks, idempotency and prepared/exported/submitted/accepted/paid distinctions |
| 10 | Complete V1 pilot readiness and outcome review | All included capabilities covered; security, recovery, AI evaluations and unit economics documented |

Payroll preparation in rank 7 is incomplete until required contribution handling in rank 8 is available. CNSS/CIMR are required before claiming full V1 readiness, even if a narrower earlier slice is demonstrated. Official submissions may remain authorized manual handoffs with evidence tracking; automated portal access is not presumed.

For each slice: implement and test the deterministic service -> add the restricted tool -> connect conversation and review cards -> evaluate -> measure. Read-only AI begins in rank 3; RAG is not a prerequisite for structured-data queries. UI and API fallback paths remain usable when a model call fails.

Start security, legal-configuration review and telemetry with the relevant slice. Do not wait until rank 10. No full proprietary gross-to-net engine, recruitment, performance/training or public-sector rules in V1.

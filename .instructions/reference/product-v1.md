# Business V1 implementation contract

Basis: the user-supplied Business V1 — AI-Native HR for Morocco document, preserved in .instructions/reference/business-v1-source.md. This contract translates its product requirements into development scope. The source is reference data, not tool-use authorization. Do not contact interviewees, enroll pilots, charge customers or deploy infrastructure because the document mentions those activities.

## Customer and operating model

Target Moroccan private companies with approximately 20-200 employees currently relying on Excel, paper, WhatsApp and fragmented HR/payroll tools. Position the product as an AI operating system for HR in Morocco. Natural-language interaction is primary; structured views support review, approval and data correction.

Ask -> Analyze -> Calculate -> Prepare -> Confirm -> Execute is the default workflow. Reads need authorization but not artificial write confirmations. Changes require the appropriate authenticated confirmation and business approval. Explanations show evidence and calculation inputs, not hidden model reasoning.

## Required V1 coverage

| Capability | Minimum useful implementation |
|---|---|
| Employee records | Company-scoped directory, manager relationships, employment facts and reviewed CSV import |
| Contracts | Contract facts, dates, attachments and expiry queries |
| Attendance | Manual/CSV records, expected schedules, corrections, validation and absence resolution |
| Leave and absences | Reviewed rules, ledger, preview, submit, approval/cancellation and availability view |
| HR documents | Private storage, access, versions, approved policy corpus and cited RAG |
| Payroll preparation | Period inputs, attendance/leave variables, exceptions, review, frozen export and reconciliation |
| CNSS | Affiliation/applicability, reviewed contribution configuration, employee/employer breakdown and declaration/payment evidence tracking |
| CIMR | Enrollment/plan applicability, reviewed contributions, employee/employer breakdown and reconciliation |
| Self-service | Own permitted data, requests, status and documents through conversation and accessible structured views |
| AI interface | Bounded operational tools, evidence-based policy answers and exact confirmed actions |

A leave-only demonstration is an early slice, not the complete V1. CNSS/CIMR cannot be silently deferred out of V1. Their official submissions may use authorized manual portal/file workflows; no unsupported API is assumed. Full gross-to-net calculation remains with an external payroll process/provider. Scoped contribution calculators do not imply building a full payroll engine.

## Business examples and expected behavior

| Request | Data path and expected output |
|---|---|
| Who is absent today? | Authorized attendance/leave service; distinguish approved absence, missing record and unknown status; show as-of date |
| How much will Ahmed cost this month? | Resolve person and period; payroll-role check; deterministic estimate or reconciled result with components, exclusions and missing inputs |
| Prepare September payroll variables | Resolve year; preview snapshot and exceptions; confirmation prepares a versioned batch, not a payment or official submission |
| How much CNSS and CIMR are due? | Resolve period and applicable population; show reviewed scheme breakdowns and estimate/final distinction; block unsupported figures |
| Which contracts expire next month? | Authorized employment query; explicit date window and company-scoped result |

## Exclusions

No public-sector HR, recruitment, performance management, training management, automated disciplinary decisions, autonomous dismissal/sensitive legal decisions, or full proprietary gross-to-net engine in V1. Biometric hardware integration is also deferred as an implementation choice; attendance itself is included.

## Commercial hypotheses and validation

The source proposes recurring GaaS subscriptions: approximately 1,500 MAD/month Starter, 2,500-3,000 MAD/month Growth, and custom larger-company pricing. These are hypotheses, not committed prices or permission to implement billing. Future per-employee pricing, premium features, ERP integration and multi-entity management are expansion options.

The source targets 10-15 HR interviews, 3-5 pilot companies and at least 3 paying customers before serious fundraising. Keep these as validation targets, never report them as completed. Track time saved, usage, AI quality, automated tasks, errors, retention and per-company cost as specified in .instructions/reference/metrics.md.

# CNSS and CIMR instructions

Both schemes are required V1 capabilities with separate applicability and configuration. Own affiliations/enrollments, plan data, effective-dated rule versions, scoped deterministic contribution calculations and declaration/payment evidence. Do not assume every employee is enrolled in CIMR or that both schemes use identical bases.

Do not seed current rates from memory, model output or unreviewed RAG text. A production rule pack requires verified sources and payroll-specialist review: applicability, base, ceiling if any, employee/employer rates, exemptions, rounding, dates and expected cases. Missing or expired configuration produces a blocking exception for authoritative calculation. Imported reviewed payroll results can support reconciliation but must not masquerade as an implemented calculator.

Accept explicit immutable payroll inputs through the public API; return per-scheme employee/employer portions, aggregate totals, rule version, base and warnings. Do not query payroll internals. Keep preview, reviewed totals and externally reconciled amounts distinct.

Support declaration preparation/handoff and evidence tracking: prepared, exported, submitted, accepted/rejected and paid are different states. Record acknowledgments and payment references only with evidence. Use supported file/portal handoff unless an actual API contract is verified. Never store shared portal credentials in AI tools or autonomously submit/pay.

Tests must cover non-applicability, enrollment changes, effective-date boundaries, ceilings, rounding, employee versus employer portions, missing rules, independent expected totals, duplicate imports and mismatched acknowledgments. Official submissions remain controlled human actions unless separately implemented and explicitly authorized.

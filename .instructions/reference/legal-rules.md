# HR legal rule configuration

The earlier Moroccan private and public HR documents are reference specifications, not executable or certified current law. Never seed guessed rates, leave entitlements or payroll formulas as production defaults.

Every active rule needs a source instrument/article, employer regime, employee category, counting unit, effective interval, version and review status. For CNSS/CIMR, also record scheme applicability, affiliation, contribution base, ceiling where applicable, rates, employee/employer portions, rounding and independently reviewed calculation examples. Treat CIMR enrollment and plan configuration explicitly; do not presume universal enrollment. RAG explanations do not establish current contribution parameters. Store test scenarios with independently reviewed expected results. Mark development fixtures as synthetic and block their accidental production activation.

Keep private and public profiles distinct. More favorable company benefits need explicit comparison in equivalent units. Model date counting, eligibility, accrual, carryover and pay consequences separately. Preserve the rule snapshot used for a decision; later changes must not silently rewrite history.

Java services calculate authoritative values. RAG can explain an applicable source but cannot change configuration or resolve an ambiguous legal conflict autonomously. If a required rule is unreviewed, return a clear configuration state and continue independent implementation work.

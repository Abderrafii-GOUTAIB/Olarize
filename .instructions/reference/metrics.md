# Validation and SaaS economics

Measure the outcomes named in Business V1. Instrument events as features ship; do not collect raw prompts, salary values or confidential employee details for analytics. Use pseudonymous actor identifiers, company ID, workflow, timestamps, outcome, failure category and provider usage metadata with scoped access and retention.

| Metric | Definition and caveat |
|---|---|
| Time saved | Compare the same task and workload before/after adoption using measured or clearly labeled self-reported baselines; include review and correction time |
| Usage frequency | Weekly active companies/users and completed workflows per company; distinguish logins from useful work |
| AI quality | Report tool/argument correctness, factual grounding, numeric agreement, authorization safety and task completion separately on reviewed cases |
| Tasks automated | Count successfully completed bounded tasks, distinguishing prepared drafts from executed actions; deduplicate retries |
| Error reduction | Compare corrections/rejections per comparable payroll cycle or task volume; document sample size |
| Retention | Track pilot continuation and later paying-customer cohorts separately; do not extrapolate from tiny samples |
| AI and infrastructure cost | Attribute model usage and job compute where possible; explicitly allocate shared hosting and storage costs per company |
| Unit economics | Compare actual revenue with attributable recurring delivery costs; distinguish contribution margin from profit and include support effort explicitly |

Store rate-card versions and usage units for cost estimation; do not hard-code model pricing. Reconcile estimates against provider invoices. Set tenant/model budgets, timeouts, rate limits and circuit breakers with clear user-facing outcomes.

Commercial hypotheses: Starter ~1,500 MAD/month, Growth ~2,500-3,000 MAD/month and custom larger-company pricing. Taxes, included employee counts, billing terms, usage quotas and payment integration remain decisions to validate. Do not enforce invented plan limits or display hypotheses as an active offer.

Validation targets: 10-15 HR-manager interviews, 3-5 pilots and at least 3 paying companies before serious fundraising. Record actual evidence and dates independently. The source example of 60 hours of administrative work is a sales illustration, not a measured customer baseline or a promise of savings.

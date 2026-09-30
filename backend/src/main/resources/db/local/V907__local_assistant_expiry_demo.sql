-- Local-only table grant for roles created after the shared migrations.
GRANT SELECT, INSERT ON assistant_conversation TO olarize_runtime;
-- Retained synthetic October 2026 contract; use the explicit reviewed question for stable Swagger results.
INSERT INTO employment_contract (id, company_id, employee_id, episode_id, current_version, lock_version)
VALUES ('c0000000-0000-4000-8000-000000000090', 'c0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000070', 'c0000000-0000-4000-8000-000000000071', 1, 0);
INSERT INTO employment_contract_version
    (id, contract_id, company_id, employee_id, episode_id, version, type_code, start_date, end_date,
     source_reference, review_state, actor_subject, operation, correlation_id, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000091', 'c0000000-0000-4000-8000-000000000090',
        'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000071', 1, 'CDD', DATE '2026-09-01', DATE '2026-10-15',
        'Synthetic expiry assistant example DEMO-CONTRACT-0002', 'UNREVIEWED', 'local-demo-seed', 'CREATE', 'local-demo-seed', now());

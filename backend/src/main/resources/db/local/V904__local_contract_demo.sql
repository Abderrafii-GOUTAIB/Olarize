-- Persistent synthetic contract facts for local Swagger correction examples only.
GRANT SELECT, INSERT, UPDATE ON employment_contract TO olarize_runtime;
GRANT SELECT, INSERT ON employment_contract_version, employment_contract_request TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.olarize_can_write_contract(uuid) TO olarize_runtime;

INSERT INTO employment_contract
    (id, company_id, employee_id, episode_id, current_version, lock_version)
VALUES ('c0000000-0000-4000-8000-000000000073',
        'c0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000071', 1, 0);

INSERT INTO employment_contract_version
    (id, contract_id, company_id, employee_id, episode_id, version, type_code,
     start_date, end_date, source_reference, review_state, actor_subject, operation,
     change_reason, evidence_reference, correlation_id, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000074',
        'c0000000-0000-4000-8000-000000000073',
        'c0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000071', 1, 'CDD',
        DATE '2026-09-01', DATE '2027-08-31',
        'Synthetic HR archive item DEMO-CONTRACT-0001', 'UNREVIEWED',
        'local-demo-seed', 'CREATE', NULL, NULL, 'local-demo-seed', now());

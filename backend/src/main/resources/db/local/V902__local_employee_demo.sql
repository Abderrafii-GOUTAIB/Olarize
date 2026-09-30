-- Persistent synthetic data for local Swagger; shared/prod Flyway never loads db/local.
GRANT SELECT, INSERT ON employment_employee, employment_episode,
    employment_creation_audit, employment_creation_request TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.olarize_can_write_employee(uuid) TO olarize_runtime;

INSERT INTO identity_membership
    (id, company_id, subject, permissions, status, version, created_at, updated_at, role_bundle)
VALUES ('c0000000-0000-4000-8000-000000000060',
        'c0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000061',
        ARRAY['employee.read', 'employee.write', 'contract.read', 'contract.write', 'employee.import'],
        'ACTIVE', 1, now(), now(), 'HR_ADMIN');

INSERT INTO identity_membership_audit
    (id, membership_id, company_id, subject, operator_name, action, reason, evidence,
     expected_version, result_version, result_status, idempotency_key, correlation_id, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000062',
        'c0000000-0000-4000-8000-000000000060',
        'c0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000061',
        'local-demo-seed', 'GRANT', 'Synthetic HR access for local Swagger',
        'Local-only seed migration', 0, 1, 'ACTIVE',
        'c0000000-0000-4000-8000-000000000063',
        'c0000000-0000-4000-8000-000000000064', now());

INSERT INTO employment_employee
    (id, company_id, reference, normalized_reference, name, job_title, department,
     site, work_email, work_phone, version, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000001',
        'EMP-0001', 'emp-0001', 'Yasmine Bennani', 'HR Coordinator',
        'People Operations', 'Casablanca', 'yasmine@demo.olarize.invalid',
        '+212 5 22 00 00 01', 1, now());

INSERT INTO employment_episode
    (id, employee_id, company_id, start_date, version, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000071',
        'c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000001', DATE '2026-09-01', 1, now());

INSERT INTO employment_creation_audit
    (id, employee_id, company_id, actor_subject, operation, result_version, correlation_id, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000072',
        'c0000000-0000-4000-8000-000000000070',
        'c0000000-0000-4000-8000-000000000001',
        'local-demo-seed', 'CREATE', 1, 'local-demo-seed', now());

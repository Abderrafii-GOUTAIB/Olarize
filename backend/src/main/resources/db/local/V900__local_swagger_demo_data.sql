-- Synthetic local-only records for Swagger checks. This location is enabled only
-- by application-local.yml and is never included in shared environment migrations.
INSERT INTO organization_company (id, name, version, created_at, updated_at)
VALUES ('c0000000-0000-4000-8000-000000000001', 'Demo Morocco SARL', 1,
        TIMESTAMPTZ '2026-01-01 09:00:00+00', TIMESTAMPTZ '2026-01-01 09:00:00+00');

INSERT INTO organization_company_creation
    (company_id, operator_name, idempotency_key, reason, action, recorded_at, correlation_id)
VALUES ('c0000000-0000-4000-8000-000000000001', 'local-demo-seed',
        'c0000000-0000-4000-8000-000000000002', 'Synthetic local Swagger demonstration data',
        'COMPANY_CREATED', TIMESTAMPTZ '2026-01-01 09:00:00+00',
        'c0000000-0000-4000-8000-000000000003');

-- The administrator subject matches the local Keycloak demo account documented
-- in docs/authentication.md. The second subject demonstrates ordinary membership.
INSERT INTO identity_membership
    (id, company_id, subject, permissions, status, version, created_at, updated_at, role_bundle)
VALUES
    ('c0000000-0000-4000-8000-000000000010', 'c0000000-0000-4000-8000-000000000001',
     'c0000000-0000-4000-8000-000000000011', ARRAY['identity.admin'], 'ACTIVE', 1,
     TIMESTAMPTZ '2026-01-01 09:00:00+00', TIMESTAMPTZ '2026-01-01 09:00:00+00', NULL),
    ('c0000000-0000-4000-8000-000000000020', 'c0000000-0000-4000-8000-000000000001',
     'c0000000-0000-4000-8000-000000000021', ARRAY['employee.self.read'], 'ACTIVE', 1,
     TIMESTAMPTZ '2026-01-01 09:00:00+00', TIMESTAMPTZ '2026-01-01 09:00:00+00', 'EMPLOYEE');

INSERT INTO identity_membership_audit
    (id, membership_id, company_id, subject, operator_name, action, reason, evidence,
     expected_version, result_version, result_status, idempotency_key, correlation_id, recorded_at)
VALUES
    ('c0000000-0000-4000-8000-000000000030', 'c0000000-0000-4000-8000-000000000010',
     'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011',
     'local-demo-seed', 'GRANT', 'Synthetic local Swagger demonstration data',
     'Local-only seed migration', 0, 1, 'ACTIVE', 'c0000000-0000-4000-8000-000000000031',
     'c0000000-0000-4000-8000-000000000032', TIMESTAMPTZ '2026-01-01 09:00:00+00'),
    ('c0000000-0000-4000-8000-000000000040', 'c0000000-0000-4000-8000-000000000020',
     'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000021',
     'local-demo-seed', 'GRANT', 'Synthetic local Swagger demonstration data',
     'Local-only seed migration', 0, 1, 'ACTIVE', 'c0000000-0000-4000-8000-000000000041',
     'c0000000-0000-4000-8000-000000000042', TIMESTAMPTZ '2026-01-01 09:00:00+00');

INSERT INTO identity_membership_change_audit
    (id, membership_id, company_id, subject, actor_subject, action,
     previous_role_bundle, next_role_bundle, previous_permissions, next_permissions,
     previous_status, next_status, reason, expected_actor_version, expected_target_version,
     result_version, result_permissions, result_status, result_role_bundle,
     idempotency_key, correlation_id, recorded_at)
VALUES
    ('c0000000-0000-4000-8000-000000000050', 'c0000000-0000-4000-8000-000000000020',
     'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000021',
     'c0000000-0000-4000-8000-000000000011', 'GRANT', NULL, 'EMPLOYEE', ARRAY[]::text[],
     ARRAY['employee.self.read'], NULL, 'ACTIVE', 'Synthetic local Swagger demonstration data',
     1, 0, 1, ARRAY['employee.self.read'], 'ACTIVE', 'EMPLOYEE',
     'c0000000-0000-4000-8000-000000000051', 'local-demo-seed', TIMESTAMPTZ '2026-01-01 09:00:00+00');

-- The local demo administrator is explicitly granted HR access as a separate
-- permission so the same synthetic login can exercise employee creation.
DELETE FROM identity_membership_change_audit
WHERE membership_id = 'c0000000-0000-4000-8000-000000000060';
DELETE FROM identity_membership_audit
WHERE membership_id = 'c0000000-0000-4000-8000-000000000060';
DELETE FROM identity_membership
WHERE id = 'c0000000-0000-4000-8000-000000000060'
  AND subject = 'c0000000-0000-4000-8000-000000000061';

UPDATE identity_membership
SET permissions = ARRAY['identity.admin', 'employee.read', 'employee.write',
                       'contract.read', 'contract.write', 'employee.import'],
    role_bundle = 'HR_ADMIN',
    version = version + 1,
    updated_at = now()
WHERE id = 'c0000000-0000-4000-8000-000000000010'
  AND company_id = 'c0000000-0000-4000-8000-000000000001'
  AND subject = 'c0000000-0000-4000-8000-000000000011'
  AND NOT permissions @> ARRAY['employee.write']::text[];

INSERT INTO identity_membership_change_audit
    (id, membership_id, company_id, subject, actor_subject, action,
     previous_role_bundle, next_role_bundle, previous_permissions, next_permissions,
     previous_status, next_status, reason, expected_actor_version, expected_target_version,
     result_version, result_permissions, result_status, result_role_bundle,
     idempotency_key, correlation_id, recorded_at)
SELECT 'c0000000-0000-4000-8000-000000000064', id, company_id, subject, 'local-demo-seed', 'UPDATE',
       NULL, 'HR_ADMIN', ARRAY['identity.admin']::text[],
       ARRAY['identity.admin', 'employee.read', 'employee.write', 'contract.read', 'contract.write', 'employee.import']::text[],
       'ACTIVE', 'ACTIVE', 'Explicit synthetic HR permission for local Swagger testing',
       1, 1, version, permissions, status, role_bundle,
       'c0000000-0000-4000-8000-000000000065', 'local-demo-seed-hr-grant', now()
FROM identity_membership
WHERE id = 'c0000000-0000-4000-8000-000000000010'
  AND company_id = 'c0000000-0000-4000-8000-000000000001'
  AND subject = 'c0000000-0000-4000-8000-000000000011'
  AND permissions @> ARRAY['employee.write']::text[]
ON CONFLICT (actor_subject, company_id, idempotency_key) DO NOTHING;

-- Explicit synthetic link authority and demo relationship, local profile only.
GRANT SELECT, INSERT, UPDATE ON employment_account_link TO olarize_runtime;
GRANT SELECT, INSERT ON employment_account_link_audit TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.olarize_can_administer_employee_link(uuid),
    public.olarize_employee_link_membership(uuid,uuid,boolean),
    public.olarize_employee_link_target_allowed(uuid,uuid,boolean),
    public.olarize_employee_link_target_exists(uuid,uuid) TO olarize_runtime;

INSERT INTO identity_membership_change_audit
    (id, membership_id, company_id, subject, actor_subject, action,
     previous_role_bundle, next_role_bundle, previous_permissions, next_permissions,
     previous_status, next_status, reason, expected_actor_version, expected_target_version,
     result_version, result_permissions, result_status, result_role_bundle,
     idempotency_key, correlation_id, recorded_at)
SELECT 'c0000000-0000-4000-8000-000000000095', id, company_id, subject, 'local-demo-seed', 'UPDATE',
       role_bundle, role_bundle, permissions, array_append(permissions, 'identity.link.admin'),
       status, status, 'Explicit synthetic link administrator for local Swagger', version, version,
       version + 1, array_append(permissions, 'identity.link.admin'), status, role_bundle,
       'c0000000-0000-4000-8000-000000000096', 'local-demo-link-grant', now()
FROM identity_membership WHERE id = 'c0000000-0000-4000-8000-000000000010';
UPDATE identity_membership SET permissions = array_append(permissions, 'identity.link.admin'),
    version = version + 1, updated_at = now()
WHERE id = 'c0000000-0000-4000-8000-000000000010';

INSERT INTO employment_account_link (id, company_id, membership_id, employee_id, status, version, recorded_at)
VALUES ('c0000000-0000-4000-8000-000000000097', 'c0000000-0000-4000-8000-000000000001',
    'c0000000-0000-4000-8000-000000000020', 'c0000000-0000-4000-8000-000000000070', 'LINKED', 1, now());
INSERT INTO employment_account_link_audit
    (id, company_id, membership_id, employee_id, status, previous_status, version, expected_version,
     membership_version, action, actor_subject, reason, verification_evidence_reference,
     correlation_id, idempotency_key, payload_hash, recorded_at)
SELECT 'c0000000-0000-4000-8000-000000000098', company_id, membership_id, employee_id, status,
    'UNLINKED', version, 0, 1, 'LINK', 'local-demo-seed',
    'Explicit synthetic relationship for local Swagger; no production verification claim',
    'local-only-synthetic-keycloak-subject-0021', 'local-demo-link-seed',
    'c0000000-0000-4000-8000-000000000097', repeat('0', 64), recorded_at
FROM employment_account_link WHERE id = 'c0000000-0000-4000-8000-000000000097';

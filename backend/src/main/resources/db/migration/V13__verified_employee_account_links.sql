-- Guarded identity metadata contracts. Ownership follows the existing restricted
-- identity function owner; runtime gets EXECUTE only, never membership writes.
CREATE FUNCTION public.olarize_can_administer_employee_link(p_company uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT p_company::text = current_setting('olarize.company_id', true)
       AND public.olarize_verified_subject() IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.identity_membership m WHERE m.company_id = p_company
           AND m.subject = public.olarize_verified_subject() AND m.status = 'ACTIVE'
           AND m.permissions @> ARRAY['identity.link.admin']::text[])
$$;
REVOKE ALL ON FUNCTION public.olarize_can_administer_employee_link(uuid) FROM PUBLIC;

CREATE FUNCTION public.olarize_employee_link_membership(p_company uuid, p_membership uuid, p_lock boolean)
RETURNS TABLE(membership_id uuid, subject varchar, version bigint, status text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
BEGIN
    IF NOT public.olarize_can_administer_employee_link(p_company) THEN
        RAISE EXCEPTION 'Account link administration denied' USING ERRCODE = '42501';
    END IF;
    IF p_lock THEN
        -- Row locks serialize membership revocation/replacement with link confirmation.
        -- Use a fixed order for actor/target to avoid opposing link-administrator races.
        PERFORM 1 FROM public.identity_membership m WHERE m.company_id = p_company
            AND (m.id = p_membership OR m.subject = public.olarize_verified_subject())
            ORDER BY m.id FOR SHARE;
        IF NOT public.olarize_can_administer_employee_link(p_company) THEN
            RAISE EXCEPTION 'Account link administration denied' USING ERRCODE = '42501';
        END IF;
    END IF;
    RETURN QUERY SELECT m.id, m.subject, m.version, m.status FROM public.identity_membership m
        WHERE m.company_id = p_company AND m.id = p_membership;
END $$;
REVOKE ALL ON FUNCTION public.olarize_employee_link_membership(uuid,uuid,boolean) FROM PUBLIC;

CREATE FUNCTION public.olarize_employee_link_target_allowed(p_company uuid, p_membership uuid, p_active boolean)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT public.olarize_can_administer_employee_link(p_company)
       AND EXISTS (SELECT 1 FROM public.identity_membership m WHERE m.company_id = p_company AND m.id = p_membership
           AND m.subject <> public.olarize_verified_subject() AND (NOT p_active OR m.status = 'ACTIVE'))
$$;
REVOKE ALL ON FUNCTION public.olarize_employee_link_target_allowed(uuid,uuid,boolean) FROM PUBLIC;

CREATE FUNCTION public.olarize_employee_link_target_exists(p_company uuid, p_employee uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT public.olarize_can_administer_employee_link(p_company)
       AND EXISTS (SELECT 1 FROM public.employment_employee e WHERE e.company_id = p_company AND e.id = p_employee)
$$;
REVOKE ALL ON FUNCTION public.olarize_employee_link_target_exists(uuid,uuid) FROM PUBLIC;
CREATE POLICY employee_link_function_read ON employment_employee FOR SELECT
    USING (current_user = 'olarize_identity_provisioner_owner'
        AND company_id::text = current_setting('olarize.company_id', true));

CREATE TABLE employment_account_link (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('LINKED','REVOKED')),
    version bigint NOT NULL CHECK (version > 0),
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employee_account_link_member_fk FOREIGN KEY (membership_id,company_id) REFERENCES identity_membership(id,company_id),
    CONSTRAINT employee_account_link_employee_fk FOREIGN KEY (employee_id,company_id) REFERENCES employment_employee(id,company_id),
    CONSTRAINT employee_account_link_member_unique UNIQUE (company_id,membership_id)
);
CREATE UNIQUE INDEX employee_account_link_employee_unique ON employment_account_link(company_id,employee_id) WHERE status = 'LINKED';

CREATE TABLE employment_account_link_audit (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    previous_employee_id uuid,
    status varchar(16) NOT NULL CHECK (status IN ('LINKED','REVOKED')),
    previous_status varchar(16) NOT NULL CHECK (previous_status IN ('UNLINKED','LINKED','REVOKED')),
    version bigint NOT NULL,
    expected_version bigint NOT NULL,
    membership_version bigint NOT NULL,
    action varchar(16) NOT NULL CHECK (action IN ('LINK','RELINK','REVOKE')),
    actor_subject varchar(255) NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    verification_evidence_reference varchar(200) NOT NULL CHECK (length(trim(verification_evidence_reference)) > 0),
    correlation_id varchar(64),
    idempotency_key uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employee_account_link_audit_member_fk FOREIGN KEY (membership_id,company_id) REFERENCES identity_membership(id,company_id),
    CONSTRAINT employee_account_link_audit_employee_fk FOREIGN KEY (employee_id,company_id) REFERENCES employment_employee(id,company_id),
    CONSTRAINT employee_account_link_audit_previous_fk FOREIGN KEY (previous_employee_id,company_id) REFERENCES employment_employee(id,company_id),
    CONSTRAINT employee_account_link_retry_unique UNIQUE (company_id,actor_subject,idempotency_key)
);
CREATE INDEX employee_account_link_audit_company_idx ON employment_account_link_audit(company_id,membership_id);
ALTER TABLE employment_account_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_account_link FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_account_link_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_account_link_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_account_link_admin_read ON employment_account_link FOR SELECT
    USING (public.olarize_can_administer_employee_link(company_id));
CREATE POLICY employee_account_link_own_read ON employment_account_link FOR SELECT
    USING (company_id::text = current_setting('olarize.company_id', true)
        AND EXISTS (SELECT 1 FROM identity_membership m WHERE m.id = employment_account_link.membership_id AND m.company_id = employment_account_link.company_id
            AND m.subject = public.olarize_verified_subject() AND m.status = 'ACTIVE'));
CREATE POLICY employee_account_link_insert ON employment_account_link FOR INSERT
    WITH CHECK (public.olarize_employee_link_target_allowed(company_id,membership_id,true));
CREATE POLICY employee_account_link_update ON employment_account_link FOR UPDATE
    USING (public.olarize_can_administer_employee_link(company_id))
    WITH CHECK (public.olarize_employee_link_target_allowed(company_id,membership_id,status = 'LINKED'));
CREATE POLICY employee_account_link_audit_read ON employment_account_link_audit FOR SELECT
    USING (public.olarize_can_administer_employee_link(company_id));
CREATE POLICY employee_account_link_audit_insert ON employment_account_link_audit FOR INSERT
    WITH CHECK (public.olarize_can_administer_employee_link(company_id)
        AND actor_subject = public.olarize_verified_subject());

DO $$ BEGIN
    IF to_regrole('olarize_identity_provisioner_owner') IS NOT NULL THEN
        GRANT SELECT ON employment_employee TO olarize_identity_provisioner_owner;
        GRANT EXECUTE ON FUNCTION public.olarize_can_read_employee(uuid), public.olarize_can_write_employee(uuid),
            public.olarize_can_read_contract(uuid)
            TO olarize_identity_provisioner_owner;
        ALTER FUNCTION public.olarize_can_administer_employee_link(uuid) OWNER TO olarize_identity_provisioner_owner;
        ALTER FUNCTION public.olarize_employee_link_membership(uuid,uuid,boolean) OWNER TO olarize_identity_provisioner_owner;
        ALTER FUNCTION public.olarize_employee_link_target_allowed(uuid,uuid,boolean) OWNER TO olarize_identity_provisioner_owner;
        ALTER FUNCTION public.olarize_employee_link_target_exists(uuid,uuid) OWNER TO olarize_identity_provisioner_owner;
    END IF;
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT SELECT,INSERT,UPDATE ON employment_account_link TO olarize_runtime;
        GRANT SELECT,INSERT ON employment_account_link_audit TO olarize_runtime;
        GRANT EXECUTE ON FUNCTION public.olarize_can_administer_employee_link(uuid),
            public.olarize_employee_link_membership(uuid,uuid,boolean),
            public.olarize_employee_link_target_allowed(uuid,uuid,boolean),
            public.olarize_employee_link_target_exists(uuid,uuid) TO olarize_runtime;
    END IF;
END $$;

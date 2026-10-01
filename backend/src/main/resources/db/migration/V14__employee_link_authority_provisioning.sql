-- Dedicated operator capability; ordinary membership bundles remain unchanged.
CREATE TABLE public.identity_employee_link_authority_audit (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    operator_name text NOT NULL,
    action text NOT NULL CHECK (action IN ('GRANT','REVOKE')),
    previous_permissions text[] NOT NULL,
    result_permissions text[] NOT NULL,
    expected_version bigint NOT NULL,
    result_version bigint NOT NULL,
    reason varchar(1000) NOT NULL,
    evidence varchar(1000) NOT NULL,
    idempotency_key uuid NOT NULL,
    correlation_id uuid NOT NULL,
    recorded_at timestamptz NOT NULL,
    FOREIGN KEY (membership_id,company_id) REFERENCES public.identity_membership(id,company_id),
    UNIQUE (operator_name,company_id,idempotency_key)
);
REVOKE ALL ON public.identity_employee_link_authority_audit FROM PUBLIC;
ALTER TABLE public.identity_employee_link_authority_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_employee_link_authority_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY link_authority_audit_owner_read ON public.identity_employee_link_authority_audit FOR SELECT
    USING (current_user = 'olarize_identity_provisioner_owner' AND operator_name = session_user);
CREATE POLICY link_authority_audit_owner_insert ON public.identity_employee_link_authority_audit FOR INSERT
    WITH CHECK (current_user = 'olarize_identity_provisioner_owner' AND operator_name = session_user);

CREATE FUNCTION public.provision_employee_link_authority(p_company uuid, p_membership uuid, p_action text,
    p_expected_version bigint, p_reason text, p_evidence text, p_key uuid, p_correlation uuid)
RETURNS TABLE (membership_id uuid, version bigint, permissions text[])
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
DECLARE
    v_previous public.identity_employee_link_authority_audit%ROWTYPE;
    v_member public.identity_membership%ROWTYPE;
    v_before text[];
BEGIN
    IF NOT coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false)
       OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user AND (rolsuper OR rolbypassrls))
       OR EXISTS (SELECT 1 FROM pg_class WHERE oid IN ('public.identity_membership'::regclass,
            'public.identity_employee_link_authority_audit'::regclass)
            AND pg_has_role(session_user, relowner, 'MEMBER')) THEN
        RAISE EXCEPTION 'Authority provisioning denied' USING ERRCODE = '42501';
    END IF;
    IF p_company IS NULL OR p_membership IS NULL OR p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE')
       OR p_expected_version IS NULL OR p_expected_version < 1 OR p_reason IS NULL OR btrim(p_reason) = ''
       OR length(p_reason) > 1000 OR p_evidence IS NULL OR btrim(p_evidence) = '' OR length(p_evidence) > 1000
       OR p_key IS NULL OR p_correlation IS NULL THEN
        RAISE EXCEPTION 'Invalid authority input' USING ERRCODE = '22023';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtextextended(session_user || ':' || p_company::text || ':LINK_AUTHORITY:' || p_key::text, 0));
    SELECT * INTO v_previous FROM public.identity_employee_link_authority_audit a
        WHERE a.operator_name = session_user AND a.company_id = p_company AND a.idempotency_key = p_key;
    IF FOUND THEN
        IF v_previous.membership_id IS DISTINCT FROM p_membership OR v_previous.action IS DISTINCT FROM p_action
           OR v_previous.expected_version IS DISTINCT FROM p_expected_version
           OR v_previous.reason IS DISTINCT FROM btrim(p_reason) OR v_previous.evidence IS DISTINCT FROM btrim(p_evidence) THEN
            RAISE EXCEPTION 'Conflicting authority retry' USING ERRCODE = 'P0002';
        END IF;
        RETURN QUERY SELECT v_previous.membership_id, v_previous.result_version, v_previous.result_permissions;
        RETURN;
    END IF;
    SELECT * INTO v_member FROM public.identity_membership m
        WHERE m.company_id = p_company AND m.id = p_membership FOR UPDATE;
    IF NOT FOUND OR v_member.version <> p_expected_version
       OR (p_action = 'GRANT' AND (v_member.status <> 'ACTIVE' OR v_member.permissions @> ARRAY['identity.link.admin']::text[]))
       OR (p_action = 'REVOKE' AND NOT v_member.permissions @> ARRAY['identity.link.admin']::text[]) THEN
        RAISE EXCEPTION 'Stale authority membership' USING ERRCODE = 'P0002';
    END IF;
    v_before := v_member.permissions;
    -- Preserve role, status and every unrelated capability. Never create/reactivate membership.
    UPDATE public.identity_membership m SET permissions = CASE p_action
        WHEN 'GRANT' THEN array_append(m.permissions, 'identity.link.admin')
        ELSE array_remove(m.permissions, 'identity.link.admin') END,
        version = m.version + 1, updated_at = transaction_timestamp()
        WHERE m.id = v_member.id RETURNING * INTO v_member;
    INSERT INTO public.identity_employee_link_authority_audit
        (id,company_id,membership_id,operator_name,action,previous_permissions,result_permissions,
         expected_version,result_version,reason,evidence,idempotency_key,correlation_id,recorded_at)
    VALUES (gen_random_uuid(),p_company,p_membership,session_user,p_action,v_before,v_member.permissions,
        p_expected_version,v_member.version,btrim(p_reason),btrim(p_evidence),p_key,p_correlation,transaction_timestamp());
    RETURN QUERY SELECT v_member.id, v_member.version, v_member.permissions;
END $$;
REVOKE ALL ON FUNCTION public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid) FROM PUBLIC;
DO $$ BEGIN
    IF to_regrole('olarize_identity_provisioner_owner') IS NOT NULL THEN
        GRANT SELECT,INSERT ON public.identity_employee_link_authority_audit TO olarize_identity_provisioner_owner;
        ALTER FUNCTION public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid)
            OWNER TO olarize_identity_provisioner_owner;
    END IF;
    IF to_regrole('olarize_identity_provisioner') IS NOT NULL THEN
        GRANT EXECUTE ON FUNCTION public.provision_employee_link_authority(uuid,uuid,text,bigint,text,text,uuid,uuid)
            TO olarize_identity_provisioner;
    END IF;
END $$;

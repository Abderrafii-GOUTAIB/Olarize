CREATE TABLE identity_membership (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES organization_company(id),
    subject varchar(255) NOT NULL CHECK (length(btrim(subject)) > 0),
    permissions text[] NOT NULL CHECK (cardinality(permissions) > 0 AND array_position(permissions, NULL) IS NULL),
    status text NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    version bigint NOT NULL CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (company_id, subject)
);
CREATE INDEX identity_membership_subject ON identity_membership(subject, status);
CREATE TABLE identity_membership_audit (
    id uuid PRIMARY KEY,
    membership_id uuid NOT NULL REFERENCES identity_membership(id),
    company_id uuid NOT NULL REFERENCES organization_company(id),
    subject varchar(255) NOT NULL,
    operator_name text NOT NULL,
    action text NOT NULL CHECK (action IN ('GRANT', 'REVOKE')),
    reason varchar(1000) NOT NULL,
    evidence varchar(1000) NOT NULL,
    expected_version bigint NOT NULL,
    result_version bigint NOT NULL,
    result_status text NOT NULL,
    idempotency_key uuid NOT NULL,
    correlation_id uuid NOT NULL,
    recorded_at timestamptz NOT NULL,
    UNIQUE (operator_name, idempotency_key)
);
REVOKE ALL ON identity_membership, identity_membership_audit FROM PUBLIC;
ALTER TABLE identity_membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_membership FORCE ROW LEVEL SECURITY;
ALTER TABLE identity_membership_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_membership_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY membership_own_read ON identity_membership FOR SELECT
    USING (subject = current_setting('olarize.subject', true)
        OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false));
CREATE POLICY membership_operator_insert ON identity_membership FOR INSERT
    WITH CHECK (coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false));
CREATE POLICY membership_operator_update ON identity_membership FOR UPDATE
    USING (coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false))
    WITH CHECK (coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false));
CREATE POLICY membership_audit_operator ON identity_membership_audit FOR ALL
    USING (operator_name = session_user) WITH CHECK (operator_name = session_user);

CREATE FUNCTION public.provision_identity_membership(
    p_company uuid, p_subject text, p_action text, p_expected_version bigint,
    p_reason text, p_evidence text, p_key uuid, p_correlation uuid
)
RETURNS TABLE (membership_id uuid, company_id uuid, version bigint, status text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
DECLARE
    v_operator text := session_user;
    v_previous public.identity_membership_audit%ROWTYPE;
    v_member public.identity_membership%ROWTYPE;
BEGIN
    IF NOT coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false)
       OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user AND (rolsuper OR rolbypassrls))
       OR EXISTS (SELECT 1 FROM pg_class WHERE oid IN ('public.identity_membership'::regclass, 'public.identity_membership_audit'::regclass)
            AND pg_has_role(session_user, relowner, 'MEMBER')) THEN
        RAISE EXCEPTION 'Provisioning denied' USING ERRCODE = '42501';
    END IF;
    IF p_company IS NULL OR p_subject IS NULL OR btrim(p_subject) = '' OR length(p_subject) > 255
       OR p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE')
       OR p_expected_version IS NULL OR p_expected_version < 0
       OR p_reason IS NULL OR btrim(p_reason) = '' OR length(p_reason) > 1000
       OR p_evidence IS NULL OR btrim(p_evidence) = '' OR length(p_evidence) > 1000
       OR p_key IS NULL OR p_correlation IS NULL THEN
        RAISE EXCEPTION 'Invalid provisioning input' USING ERRCODE = '22023';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtextextended(v_operator || ':' || p_key::text, 0));
    SELECT * INTO v_previous FROM public.identity_membership_audit
        WHERE operator_name = v_operator AND idempotency_key = p_key;
    IF FOUND THEN
        IF v_previous.company_id IS DISTINCT FROM p_company OR v_previous.subject IS DISTINCT FROM p_subject
           OR v_previous.action IS DISTINCT FROM p_action OR v_previous.expected_version IS DISTINCT FROM p_expected_version
           OR v_previous.reason IS DISTINCT FROM btrim(p_reason) OR v_previous.evidence IS DISTINCT FROM btrim(p_evidence) THEN
            RAISE EXCEPTION 'Conflicting retry' USING ERRCODE = 'P0002';
        END IF;
        RETURN QUERY SELECT v_previous.membership_id, v_previous.company_id, v_previous.result_version, v_previous.result_status;
        RETURN;
    END IF;
    PERFORM set_config('olarize.company_id', p_company::text, true);
    IF NOT EXISTS (SELECT 1 FROM public.organization_company WHERE id = p_company) THEN
        RAISE EXCEPTION 'Provisioning denied' USING ERRCODE = '42501';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtextextended(p_company::text || ':' || p_subject, 1));
    SELECT * INTO v_member FROM public.identity_membership WHERE identity_membership.company_id = p_company AND subject = p_subject FOR UPDATE;
    IF NOT FOUND THEN
        IF p_action <> 'GRANT' OR p_expected_version <> 0 THEN
            RAISE EXCEPTION 'Stale membership' USING ERRCODE = 'P0002';
        END IF;
        INSERT INTO public.identity_membership(id, company_id, subject, permissions, status, version, created_at, updated_at)
            VALUES (gen_random_uuid(), p_company, p_subject, ARRAY['identity.admin'], 'ACTIVE', 1, transaction_timestamp(), transaction_timestamp())
            RETURNING * INTO v_member;
    ELSE
        IF v_member.version <> p_expected_version OR v_member.permissions <> ARRAY['identity.admin']::text[]
            OR (p_action = 'GRANT' AND v_member.status = 'ACTIVE') OR (p_action = 'REVOKE' AND v_member.status = 'REVOKED') THEN
            RAISE EXCEPTION 'Stale membership' USING ERRCODE = 'P0002';
        END IF;
        UPDATE public.identity_membership SET status = CASE p_action WHEN 'GRANT' THEN 'ACTIVE' ELSE 'REVOKED' END,
            version = identity_membership.version + 1, updated_at = transaction_timestamp()
            WHERE id = v_member.id RETURNING * INTO v_member;
    END IF;
    INSERT INTO public.identity_membership_audit(id, membership_id, company_id, subject, operator_name, action,
        reason, evidence, expected_version, result_version, result_status, idempotency_key, correlation_id, recorded_at)
        VALUES (gen_random_uuid(), v_member.id, p_company, p_subject, v_operator, p_action, btrim(p_reason), btrim(p_evidence),
            p_expected_version, v_member.version, v_member.status, p_key, p_correlation, transaction_timestamp());
    RETURN QUERY SELECT v_member.id, v_member.company_id, v_member.version, v_member.status;
END;
$$;
REVOKE ALL ON FUNCTION public.provision_identity_membership(uuid, text, text, bigint, text, text, uuid, uuid) FROM PUBLIC;

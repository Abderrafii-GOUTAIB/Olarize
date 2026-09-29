ALTER TABLE public.identity_membership
    ADD COLUMN role_bundle text CHECK (role_bundle IS NULL OR role_bundle IN
        ('HR_ADMIN', 'MANAGER', 'EMPLOYEE', 'PAYROLL_OPERATOR'));

CREATE POLICY membership_admin_function_read ON public.identity_membership FOR SELECT
    USING (current_user = 'olarize_identity_provisioner_owner');
CREATE POLICY membership_admin_function_insert ON public.identity_membership FOR INSERT
    WITH CHECK (current_user = 'olarize_identity_provisioner_owner');
CREATE POLICY membership_admin_function_update ON public.identity_membership FOR UPDATE
    USING (current_user = 'olarize_identity_provisioner_owner')
    WITH CHECK (current_user = 'olarize_identity_provisioner_owner');

CREATE TABLE public.identity_membership_change_audit (
    id uuid PRIMARY KEY,
    membership_id uuid NOT NULL REFERENCES public.identity_membership(id),
    company_id uuid NOT NULL REFERENCES public.organization_company(id),
    subject varchar(255) NOT NULL,
    actor_subject varchar(255) NOT NULL,
    action text NOT NULL CHECK (action IN ('GRANT', 'UPDATE', 'REACTIVATE', 'REVOKE')),
    previous_role_bundle text,
    next_role_bundle text,
    previous_permissions text[] NOT NULL,
    next_permissions text[] NOT NULL,
    previous_status text,
    next_status text NOT NULL CHECK (next_status IN ('ACTIVE', 'REVOKED')),
    reason varchar(1000) NOT NULL,
    expected_actor_version bigint NOT NULL,
    expected_target_version bigint NOT NULL,
    result_version bigint NOT NULL,
    result_permissions text[] NOT NULL,
    result_status text NOT NULL CHECK (result_status IN ('ACTIVE', 'REVOKED')),
    result_role_bundle text,
    idempotency_key uuid NOT NULL,
    correlation_id varchar(64) NOT NULL,
    recorded_at timestamptz NOT NULL,
    UNIQUE (actor_subject, company_id, idempotency_key)
);
REVOKE ALL ON public.identity_membership_change_audit FROM PUBLIC;
ALTER TABLE public.identity_membership_change_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_membership_change_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY membership_change_audit_owner_only ON public.identity_membership_change_audit
    FOR ALL USING (current_user = 'olarize_identity_provisioner_owner'
        OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false))
    WITH CHECK (current_user = 'olarize_identity_provisioner_owner'
        OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false));

CREATE FUNCTION public.list_company_identity_memberships(p_actor text, p_company uuid)
RETURNS TABLE (membership_id uuid, company_id uuid, subject varchar, permissions text[], status text,
               version bigint, role_bundle text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
BEGIN
    IF p_actor IS NULL OR p_company IS NULL
       OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user AND (rolsuper OR rolbypassrls))
       OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner_owner'), 'MEMBER'), false) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    PERFORM set_config('olarize.subject', p_actor, true);
    IF NOT EXISTS (SELECT 1 FROM public.identity_membership actor
        WHERE actor.company_id = p_company AND actor.subject = p_actor AND actor.status = 'ACTIVE'
          AND 'identity.admin' = ANY(actor.permissions)) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    RETURN QUERY SELECT member.id, member.company_id, member.subject, member.permissions,
        member.status, member.version, member.role_bundle
        FROM public.identity_membership member WHERE member.company_id = p_company ORDER BY member.subject;
END;
$$;
REVOKE ALL ON FUNCTION public.list_company_identity_memberships(text, uuid) FROM PUBLIC;

CREATE FUNCTION public.change_company_membership(
    p_actor text, p_company uuid, p_subject text, p_action text, p_role_bundle text,
    p_actor_version bigint, p_target_version bigint, p_reason text, p_key uuid, p_correlation text
)
RETURNS TABLE (membership_id uuid, company_id uuid, subject varchar, permissions text[], status text,
               version bigint, role_bundle text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
DECLARE
    v_old public.identity_membership%ROWTYPE;
    v_new public.identity_membership%ROWTYPE;
    v_previous public.identity_membership_change_audit%ROWTYPE;
    v_permissions text[];
    v_actor_version bigint;
    v_action text := p_action;
    v_now timestamptz := transaction_timestamp();
BEGIN
    IF p_actor IS NULL OR btrim(p_actor) = '' OR length(p_actor) > 255 OR p_company IS NULL
       OR p_subject IS NULL OR btrim(p_subject) = '' OR length(p_subject) > 255
       OR p_actor = p_subject OR p_action IS NULL OR p_action NOT IN ('GRANT', 'UPDATE', 'REACTIVATE', 'REVOKE')
       OR p_actor_version IS NULL OR p_actor_version < 1 OR p_target_version IS NULL OR p_target_version < 0
       OR p_reason IS NULL OR btrim(p_reason) = '' OR length(p_reason) > 1000
       OR p_key IS NULL OR p_correlation IS NULL OR btrim(p_correlation) = '' OR length(p_correlation) > 64
       OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user AND (rolsuper OR rolbypassrls))
       OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner_owner'), 'MEMBER'), false) THEN
        RAISE EXCEPTION 'Membership change denied' USING ERRCODE = '42501';
    END IF;
    IF p_role_bundle IS NOT NULL AND p_role_bundle NOT IN ('HR_ADMIN', 'MANAGER', 'EMPLOYEE', 'PAYROLL_OPERATOR') THEN
        RAISE EXCEPTION 'Unknown permission bundle' USING ERRCODE = '22023';
    END IF;
    v_permissions := CASE p_role_bundle
        WHEN 'HR_ADMIN' THEN ARRAY['contract.read','contract.write','employee.import','employee.read','employee.write']::text[]
        WHEN 'MANAGER' THEN ARRAY['employee.direct-reports.read']::text[]
        WHEN 'EMPLOYEE' THEN ARRAY['employee.self.read']::text[]
        WHEN 'PAYROLL_OPERATOR' THEN ARRAY['payroll.roster.read']::text[]
        ELSE ARRAY[]::text[] END;
    IF (p_action = 'REVOKE' AND (p_role_bundle IS NOT NULL OR cardinality(v_permissions) <> 0))
       OR (p_action <> 'REVOKE' AND p_role_bundle IS NULL) THEN
        RAISE EXCEPTION 'Invalid permission bundle' USING ERRCODE = '22023';
    END IF;

    PERFORM pg_advisory_xact_lock(hashtextextended(p_actor || ':' || p_company::text || ':' || p_key::text, 0));
    SELECT actor.version INTO v_actor_version FROM public.identity_membership actor
        WHERE actor.company_id = p_company AND actor.subject = p_actor AND actor.status = 'ACTIVE'
          AND 'identity.admin' = ANY(actor.permissions) FOR UPDATE;
    IF NOT FOUND OR v_actor_version <> p_actor_version THEN
        RAISE EXCEPTION 'Membership administration denied or stale' USING ERRCODE = 'P0002';
    END IF;
    SELECT audit.* INTO v_previous FROM public.identity_membership_change_audit audit
        WHERE audit.actor_subject = p_actor AND audit.company_id = p_company AND audit.idempotency_key = p_key;
    IF FOUND THEN
        IF v_previous.subject IS DISTINCT FROM p_subject OR v_previous.action IS DISTINCT FROM v_action
           OR v_previous.next_role_bundle IS DISTINCT FROM p_role_bundle
           OR v_previous.reason IS DISTINCT FROM btrim(p_reason)
           OR v_previous.expected_actor_version <> p_actor_version
           OR v_previous.expected_target_version <> p_target_version THEN
            RAISE EXCEPTION 'Conflicting membership retry' USING ERRCODE = 'P0002';
        END IF;
        RETURN QUERY SELECT v_previous.membership_id, v_previous.company_id, v_previous.subject,
            v_previous.result_permissions, v_previous.result_status, v_previous.result_version, v_previous.result_role_bundle;
        RETURN;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtextextended(p_company::text || ':' || p_subject, 1));
    PERFORM set_config('olarize.subject', p_actor, true);
    PERFORM set_config('olarize.company_id', p_company::text, true);
    IF NOT EXISTS (SELECT 1 FROM public.organization_company WHERE id = p_company) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    SELECT * INTO v_old FROM public.identity_membership member
        WHERE member.company_id = p_company AND member.subject = p_subject FOR UPDATE;

    IF FOUND THEN
        IF v_old.permissions @> ARRAY['identity.admin']::text[] THEN
            RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
        END IF;
        IF v_old.version <> p_target_version THEN
            RAISE EXCEPTION 'Membership version is stale' USING ERRCODE = 'P0002';
        END IF;
    ELSIF p_target_version <> 0 OR p_action <> 'GRANT' THEN
        RAISE EXCEPTION 'Membership version is stale' USING ERRCODE = 'P0002';
    END IF;
    IF (p_action = 'GRANT' AND FOUND)
       OR (p_action IN ('UPDATE','REVOKE') AND (NOT FOUND OR v_old.status <> 'ACTIVE'))
       OR (p_action = 'REACTIVATE' AND (NOT FOUND OR v_old.status <> 'REVOKED')) THEN
        RAISE EXCEPTION 'Membership transition is stale' USING ERRCODE = 'P0002';
    END IF;
    IF p_action = 'REVOKE' THEN
        UPDATE public.identity_membership member SET status = 'REVOKED', version = member.version + 1, updated_at = v_now
            WHERE member.id = v_old.id RETURNING * INTO v_new;
    ELSIF p_action = 'GRANT' THEN
        INSERT INTO public.identity_membership(id, company_id, subject, permissions, status, version, created_at, updated_at, role_bundle)
            VALUES (gen_random_uuid(), p_company, p_subject, v_permissions, 'ACTIVE', 1, v_now, v_now, p_role_bundle)
            RETURNING * INTO v_new;
    ELSE
        UPDATE public.identity_membership member SET permissions = v_permissions, role_bundle = p_role_bundle,
            status = 'ACTIVE', version = member.version + 1, updated_at = v_now
            WHERE member.id = v_old.id RETURNING * INTO v_new;
    END IF;

    INSERT INTO public.identity_membership_change_audit(id, membership_id, company_id, subject, actor_subject, action,
        previous_role_bundle, next_role_bundle, previous_permissions, next_permissions, previous_status, next_status,
        reason, expected_actor_version, expected_target_version, result_version, result_permissions, result_status,
        result_role_bundle, idempotency_key, correlation_id, recorded_at)
    VALUES (gen_random_uuid(), v_new.id, p_company, p_subject, p_actor, p_action,
        CASE WHEN v_old.id IS NULL THEN NULL ELSE v_old.role_bundle END, v_new.role_bundle,
        CASE WHEN v_old.id IS NULL THEN ARRAY[]::text[] ELSE v_old.permissions END, v_new.permissions,
        CASE WHEN v_old.id IS NULL THEN NULL ELSE v_old.status END, v_new.status, btrim(p_reason),
        p_actor_version, p_target_version, v_new.version, v_new.permissions, v_new.status, v_new.role_bundle,
        p_key, p_correlation, v_now);
    RETURN QUERY SELECT v_new.id, v_new.company_id, v_new.subject, v_new.permissions,
        v_new.status, v_new.version, v_new.role_bundle;
END;
$$;
REVOKE ALL ON FUNCTION public.change_company_membership(text,uuid,text,text,text,bigint,bigint,text,uuid,text) FROM PUBLIC;

CREATE FUNCTION public.find_company_membership_change_retry(
    p_actor text, p_company uuid, p_subject text, p_action text, p_role_bundle text,
    p_actor_version bigint, p_target_version bigint, p_reason text, p_key uuid
)
RETURNS TABLE (membership_id uuid, company_id uuid, subject varchar, permissions text[], status text,
               version bigint, role_bundle text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
DECLARE
    v_actor_version bigint;
    v_previous public.identity_membership_change_audit%ROWTYPE;
BEGIN
    IF p_actor IS NULL OR p_company IS NULL OR p_key IS NULL
       OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user AND (rolsuper OR rolbypassrls))
       OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner_owner'), 'MEMBER'), false) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    SELECT actor.version INTO v_actor_version FROM public.identity_membership actor
        WHERE actor.company_id = p_company AND actor.subject = p_actor AND actor.status = 'ACTIVE'
          AND 'identity.admin' = ANY(actor.permissions);
    IF NOT FOUND OR v_actor_version <> p_actor_version THEN
        RAISE EXCEPTION 'Membership administration denied or stale' USING ERRCODE = 'P0002';
    END IF;
    SELECT audit.* INTO v_previous FROM public.identity_membership_change_audit audit
        WHERE audit.actor_subject = p_actor AND audit.company_id = p_company AND audit.idempotency_key = p_key;
    IF NOT FOUND THEN RETURN; END IF;
    IF v_previous.subject IS DISTINCT FROM p_subject OR v_previous.action IS DISTINCT FROM p_action
       OR v_previous.next_role_bundle IS DISTINCT FROM p_role_bundle
       OR v_previous.reason IS DISTINCT FROM btrim(p_reason)
       OR v_previous.expected_actor_version <> p_actor_version
       OR v_previous.expected_target_version <> p_target_version THEN
        RAISE EXCEPTION 'Conflicting membership retry' USING ERRCODE = 'P0002';
    END IF;
    RETURN QUERY SELECT v_previous.membership_id, v_previous.company_id, v_previous.subject,
        v_previous.result_permissions, v_previous.result_status, v_previous.result_version, v_previous.result_role_bundle;
END;
$$;
REVOKE ALL ON FUNCTION public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid) FROM PUBLIC;

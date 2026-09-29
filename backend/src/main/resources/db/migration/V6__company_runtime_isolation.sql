-- The application signs each verified scope with a key unavailable to the
-- runtime database role. A caller with only runtime SQL credentials cannot
-- impersonate a different Keycloak subject by setting custom GUCs.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS olarize_private;
REVOKE ALL ON SCHEMA olarize_private FROM PUBLIC;
CREATE TABLE olarize_private.scope_signing_key (
    id integer PRIMARY KEY CHECK (id = 1),
    key_bytes bytea NOT NULL CHECK (octet_length(key_bytes) >= 32)
);
REVOKE ALL ON olarize_private.scope_signing_key FROM PUBLIC;
INSERT INTO olarize_private.scope_signing_key(id, key_bytes)
VALUES (1, decode('${db_scope_key}', 'hex'));

CREATE FUNCTION public.olarize_verified_subject() RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
DECLARE
    v_subject text := current_setting('olarize.subject', true);
    v_company text := coalesce(current_setting('olarize.company_id', true), '');
    v_expiry text := current_setting('olarize.scope_expires', true);
    v_proof text := current_setting('olarize.scope_proof', true);
    v_key bytea;
    v_now bigint := extract(epoch FROM clock_timestamp())::bigint;
BEGIN
    IF v_subject IS NULL OR btrim(v_subject) = '' OR length(v_subject) > 255
       OR v_expiry IS NULL OR v_expiry !~ '^[0-9]{10}$'
       OR v_proof IS NULL OR v_proof !~ '^[0-9a-f]{64}$' THEN
        RETURN NULL;
    END IF;
    IF v_expiry::bigint < v_now OR v_expiry::bigint > v_now + 120 THEN
        RETURN NULL;
    END IF;
    SELECT key_bytes INTO v_key FROM olarize_private.scope_signing_key WHERE id = 1;
    IF v_proof IS DISTINCT FROM encode(public.hmac(
            convert_to(v_subject || E'\n' || v_company || E'\n' || v_expiry, 'UTF8'),
            v_key, 'sha256'), 'hex') THEN
        RETURN NULL;
    END IF;
    RETURN v_subject;
END;
$$;

-- The runtime may discover only its own active memberships before selecting a
-- company. Function-owner and controlled provisioning policies remain separate.
DROP POLICY membership_own_read ON public.identity_membership;
CREATE POLICY membership_own_read ON public.identity_membership FOR SELECT
    USING (subject = public.olarize_verified_subject() AND status = 'ACTIVE'
        AND (nullif(current_setting('olarize.company_id', true), '') IS NULL
            OR company_id::text = current_setting('olarize.company_id', true))
        OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false));

-- A selected company needs both transaction-local values and a current active
-- membership. The controlled provisioning role retains its existing scoped
-- lookup needed for company creation/retry before a membership exists.
DROP POLICY company_scoped_read ON public.organization_company;
CREATE POLICY company_scoped_read ON public.organization_company FOR SELECT
    USING (id::text = current_setting('olarize.company_id', true)
        AND (coalesce(pg_has_role(session_user, to_regrole('olarize_company_provisioner'), 'MEMBER'), false)
            OR coalesce(pg_has_role(session_user, to_regrole('olarize_identity_provisioner'), 'MEMBER'), false)
            OR EXISTS (SELECT 1 FROM public.identity_membership member
                WHERE member.company_id = organization_company.id
                  AND member.subject = public.olarize_verified_subject()
                  AND member.status = 'ACTIVE')));

-- Audit evidence must refer to a membership in the same company. PostgreSQL
-- foreign-key checks are not subject to RLS, so the company must be part of
-- the constraint rather than relying only on a policy.
ALTER TABLE public.identity_membership
    ADD CONSTRAINT identity_membership_id_company_unique UNIQUE (id, company_id);
ALTER TABLE public.identity_membership_audit
    ADD CONSTRAINT identity_membership_audit_company_fk
    FOREIGN KEY (membership_id, company_id)
    REFERENCES public.identity_membership(id, company_id);
ALTER TABLE public.identity_membership_change_audit
    ADD CONSTRAINT identity_membership_change_audit_company_fk
    FOREIGN KEY (membership_id, company_id)
    REFERENCES public.identity_membership(id, company_id);

-- Keep the existing audited mutation implementation, but put a signed-scope
-- guard in front of every runtime-callable administrative entry point.
ALTER FUNCTION public.list_company_identity_memberships(text, uuid)
    RENAME TO list_company_identity_memberships_internal;
ALTER FUNCTION public.change_company_membership(text,uuid,text,text,text,bigint,bigint,text,uuid,text)
    RENAME TO change_company_membership_internal;
ALTER FUNCTION public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid)
    RENAME TO find_company_membership_change_retry_internal;
REVOKE ALL ON FUNCTION public.list_company_identity_memberships_internal(text, uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.change_company_membership_internal(text,uuid,text,text,text,bigint,bigint,text,uuid,text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.find_company_membership_change_retry_internal(text,uuid,text,text,text,bigint,bigint,text,uuid) FROM PUBLIC;

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        REVOKE ALL ON FUNCTION public.list_company_identity_memberships_internal(text, uuid) FROM olarize_runtime;
        REVOKE ALL ON FUNCTION public.change_company_membership_internal(text,uuid,text,text,text,bigint,bigint,text,uuid,text) FROM olarize_runtime;
        REVOKE ALL ON FUNCTION public.find_company_membership_change_retry_internal(text,uuid,text,text,text,bigint,bigint,text,uuid) FROM olarize_runtime;
    END IF;
END;
$$;

CREATE FUNCTION public.list_company_identity_memberships(p_actor text, p_company uuid)
RETURNS TABLE (membership_id uuid, company_id uuid, subject varchar, permissions text[], status text,
               version bigint, role_bundle text)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $$
BEGIN
    IF p_actor IS DISTINCT FROM public.olarize_verified_subject()
       OR p_company::text IS DISTINCT FROM current_setting('olarize.company_id', true) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    RETURN QUERY SELECT * FROM public.list_company_identity_memberships_internal(p_actor, p_company);
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
BEGIN
    IF p_actor IS DISTINCT FROM public.olarize_verified_subject()
       OR p_company::text IS DISTINCT FROM current_setting('olarize.company_id', true) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    RETURN QUERY SELECT * FROM public.change_company_membership_internal(
        p_actor, p_company, p_subject, p_action, p_role_bundle, p_actor_version,
        p_target_version, p_reason, p_key, p_correlation);
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
BEGIN
    IF p_actor IS DISTINCT FROM public.olarize_verified_subject()
       OR p_company::text IS DISTINCT FROM current_setting('olarize.company_id', true) THEN
        RAISE EXCEPTION 'Membership administration denied' USING ERRCODE = '42501';
    END IF;
    RETURN QUERY SELECT * FROM public.find_company_membership_change_retry_internal(
        p_actor, p_company, p_subject, p_action, p_role_bundle, p_actor_version,
        p_target_version, p_reason, p_key);
END;
$$;
REVOKE ALL ON FUNCTION public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid) FROM PUBLIC;

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT EXECUTE ON FUNCTION public.list_company_identity_memberships(text, uuid) TO olarize_runtime;
        GRANT EXECUTE ON FUNCTION public.change_company_membership(text,uuid,text,text,text,bigint,bigint,text,uuid,text) TO olarize_runtime;
        GRANT EXECUTE ON FUNCTION public.find_company_membership_change_retry(text,uuid,text,text,text,bigint,bigint,text,uuid) TO olarize_runtime;
    END IF;
END;
$$;

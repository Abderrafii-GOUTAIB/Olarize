-- Local-only application login. Flyway connects as the local database bootstrap
-- administrator; the web datasource uses this restricted, non-owner role.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'olarize_runtime') THEN
        CREATE ROLE olarize_runtime LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE
            PASSWORD '${local_runtime_password}';
    ELSIF EXISTS (SELECT 1 FROM pg_roles
                  WHERE rolname = 'olarize_runtime'
                    AND (rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole)) THEN
        RAISE EXCEPTION 'Local runtime role must remain restricted';
    END IF;
END;
$$;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO olarize_runtime', current_database());
END;
$$;
GRANT USAGE ON SCHEMA public TO olarize_runtime;
GRANT SELECT ON public.organization_company, public.identity_membership TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.list_company_identity_memberships(text, uuid) TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.change_company_membership(text, uuid, text, text, text, bigint, bigint, text, uuid, text)
    TO olarize_runtime;
GRANT EXECUTE ON FUNCTION public.find_company_membership_change_retry(text, uuid, text, text, text, bigint, bigint, text, uuid)
    TO olarize_runtime;

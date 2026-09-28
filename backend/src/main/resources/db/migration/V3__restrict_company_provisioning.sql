CREATE FUNCTION public.provision_company(
    p_company_id uuid,
    p_name text,
    p_reason text,
    p_idempotency_key uuid,
    p_correlation_id uuid
)
RETURNS TABLE (
    company_id uuid,
    name text,
    version bigint,
    created_at timestamptz,
    updated_at timestamptz,
    conflict boolean
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    v_operator text := session_user;
    v_existing_company uuid;
    v_existing_reason text;
    v_name text := btrim(p_name);
    v_reason text := btrim(p_reason);
BEGIN
    IF p_company_id IS NULL OR p_idempotency_key IS NULL OR p_correlation_id IS NULL
       OR p_name IS NULL OR btrim(p_name) = '' OR char_length(p_name) > 200
       OR p_reason IS NULL OR btrim(p_reason) = '' OR char_length(p_reason) > 1000 THEN
        RAISE EXCEPTION 'Invalid company registration' USING ERRCODE = '22023';
    END IF;

    PERFORM pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(v_operator || ':' || p_idempotency_key::text, 0));

    SELECT receipt.company_id, receipt.reason
      INTO v_existing_company, v_existing_reason
      FROM public.organization_company_creation AS receipt
     WHERE receipt.operator_name = v_operator
       AND receipt.idempotency_key = p_idempotency_key;

    IF FOUND THEN
        PERFORM pg_catalog.set_config('olarize.company_id', v_existing_company::text, true);
        RETURN QUERY
        SELECT company.id, company.name::text, company.version, company.created_at, company.updated_at,
               (v_existing_reason IS DISTINCT FROM v_reason OR company.name IS DISTINCT FROM v_name)
          FROM public.organization_company AS company
         WHERE company.id = v_existing_company;
        RETURN;
    END IF;

    PERFORM pg_catalog.set_config('olarize.company_id', p_company_id::text, true);
    INSERT INTO public.organization_company(id, name, created_at, updated_at)
    VALUES (p_company_id, v_name, transaction_timestamp(), transaction_timestamp());

    INSERT INTO public.organization_company_creation(
        company_id, operator_name, idempotency_key, reason, action, recorded_at, correlation_id)
    VALUES (
        p_company_id, v_operator, p_idempotency_key, v_reason, 'COMPANY_CREATED',
        transaction_timestamp(), p_correlation_id);

    RETURN QUERY
    SELECT company.id, company.name::text, company.version, company.created_at, company.updated_at, false
      FROM public.organization_company AS company
     WHERE company.id = p_company_id;
END;
$$;

REVOKE ALL ON FUNCTION public.provision_company(uuid, text, text, uuid, uuid) FROM PUBLIC;

-- Keep role setup operational and separate from schema migrations. If the group
-- already exists, remove any old direct-write grant and grant only the function.
DO $$
BEGIN
    IF pg_catalog.to_regrole('olarize_company_provisioner') IS NOT NULL THEN
        EXECUTE 'REVOKE INSERT ON public.organization_company, public.organization_company_creation FROM olarize_company_provisioner';
        EXECUTE 'GRANT EXECUTE ON FUNCTION public.provision_company(uuid, text, text, uuid, uuid) TO olarize_company_provisioner';
    END IF;
END;
$$;

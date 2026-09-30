-- Permit only signed, current company members with contract.read to read
-- contract and employee facts. Existing write policies remain responsible for mutations.
CREATE FUNCTION public.olarize_can_read_contract(p_company uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT p_company::text = current_setting('olarize.company_id', true)
       AND public.olarize_verified_subject() IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.identity_membership m
                   WHERE m.company_id = p_company
                     AND m.subject = public.olarize_verified_subject()
                     AND m.status = 'ACTIVE'
                     AND m.permissions @> ARRAY['contract.read']::text[])
$$;
REVOKE ALL ON FUNCTION public.olarize_can_read_contract(uuid) FROM PUBLIC;

CREATE INDEX employment_contract_version_expiry_idx
    ON employment_contract_version(company_id, end_date, contract_id, version)
    WHERE end_date IS NOT NULL;
CREATE INDEX employment_contract_version_missing_end_date_idx
    ON employment_contract_version(company_id, contract_id, version)
    WHERE end_date IS NULL;

CREATE POLICY contract_read ON employment_contract FOR SELECT
    USING (public.olarize_can_read_contract(company_id));
CREATE POLICY contract_version_read ON employment_contract_version FOR SELECT
    USING (public.olarize_can_read_contract(company_id));
CREATE POLICY employee_contract_read ON employment_employee FOR SELECT
    USING (public.olarize_can_read_contract(company_id));
CREATE POLICY episode_contract_read ON employment_episode FOR SELECT
    USING (public.olarize_can_read_contract(company_id));

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT EXECUTE ON FUNCTION public.olarize_can_read_contract(uuid) TO olarize_runtime;
    END IF;
END;
$$;

-- Current employee.read membership plus signed selected-company scope is required.
CREATE FUNCTION public.olarize_can_read_employee(p_company uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT p_company::text = current_setting('olarize.company_id', true)
       AND public.olarize_verified_subject() IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.identity_membership m
                   WHERE m.company_id = p_company
                     AND m.subject = public.olarize_verified_subject()
                     AND m.status = 'ACTIVE'
                     AND m.permissions @> ARRAY['employee.read']::text[])
$$;
REVOKE ALL ON FUNCTION public.olarize_can_read_employee(uuid) FROM PUBLIC;

CREATE POLICY employee_directory_read ON employment_employee FOR SELECT
    USING (public.olarize_can_read_employee(company_id));
CREATE POLICY episode_directory_read ON employment_episode FOR SELECT
    USING (public.olarize_can_read_employee(company_id));
CREATE INDEX employment_episode_selection_idx ON employment_episode(company_id, employee_id, start_date, id);
CREATE INDEX employment_employee_directory_order_idx ON employment_employee(company_id, normalized_reference, id);

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT EXECUTE ON FUNCTION public.olarize_can_read_employee(uuid) TO olarize_runtime;
    END IF;
END;
$$;

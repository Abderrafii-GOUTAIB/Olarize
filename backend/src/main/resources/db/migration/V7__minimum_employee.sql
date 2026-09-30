CREATE TABLE employment_employee (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES organization_company(id),
    reference varchar(64) NOT NULL,
    normalized_reference varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    job_title varchar(200),
    department varchar(200),
    site varchar(200),
    work_email varchar(254),
    work_phone varchar(32),
    version bigint NOT NULL DEFAULT 1 CHECK (version = 1),
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_employee_company_id_unique UNIQUE (id, company_id),
    CONSTRAINT employment_employee_reference_unique UNIQUE (company_id, normalized_reference)
);
CREATE INDEX employment_employee_company_idx ON employment_employee(company_id);

CREATE TABLE employment_episode (
    id uuid PRIMARY KEY,
    employee_id uuid NOT NULL,
    company_id uuid NOT NULL,
    start_date date NOT NULL,
    last_employed_day date,
    version bigint NOT NULL DEFAULT 1 CHECK (version = 1),
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_episode_employee_fk FOREIGN KEY (employee_id, company_id)
        REFERENCES employment_employee(id, company_id),
    CONSTRAINT employment_episode_dates CHECK (last_employed_day IS NULL OR last_employed_day >= start_date)
);
CREATE INDEX employment_episode_company_idx ON employment_episode(company_id);

CREATE TABLE employment_creation_audit (
    id uuid PRIMARY KEY,
    employee_id uuid NOT NULL,
    company_id uuid NOT NULL,
    actor_subject varchar(255) NOT NULL,
    operation varchar(32) NOT NULL CHECK (operation = 'CREATE'),
    result_version bigint NOT NULL CHECK (result_version = 1),
    correlation_id varchar(64),
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_audit_employee_fk FOREIGN KEY (employee_id, company_id)
        REFERENCES employment_employee(id, company_id)
);
CREATE INDEX employment_audit_company_idx ON employment_creation_audit(company_id);

CREATE TABLE employment_creation_request (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    actor_subject varchar(255) NOT NULL,
    idempotency_key uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    employee_id uuid NOT NULL,
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_request_employee_fk FOREIGN KEY (employee_id, company_id)
        REFERENCES employment_employee(id, company_id),
    CONSTRAINT employment_request_key_unique UNIQUE (company_id, actor_subject, idempotency_key)
);
CREATE INDEX employment_request_company_idx ON employment_creation_request(company_id);

ALTER TABLE employment_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_employee FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_episode ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_episode FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_creation_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_creation_audit FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_creation_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_creation_request FORCE ROW LEVEL SECURITY;

-- Every protected table requires a signed selected company and current employee.write
-- membership; a settable company GUC alone never grants visibility or mutation.
CREATE FUNCTION public.olarize_can_write_employee(p_company uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT p_company::text = current_setting('olarize.company_id', true)
       AND public.olarize_verified_subject() IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.identity_membership m
                   WHERE m.company_id = p_company
                     AND m.subject = public.olarize_verified_subject()
                     AND m.status = 'ACTIVE'
                     AND m.permissions @> ARRAY['employee.write']::text[])
$$;
REVOKE ALL ON FUNCTION public.olarize_can_write_employee(uuid) FROM PUBLIC;

CREATE POLICY employee_hr ON employment_employee FOR ALL
    USING (public.olarize_can_write_employee(company_id))
    WITH CHECK (public.olarize_can_write_employee(company_id));
CREATE POLICY episode_hr ON employment_episode FOR ALL
    USING (public.olarize_can_write_employee(company_id))
    WITH CHECK (public.olarize_can_write_employee(company_id));
CREATE POLICY employee_audit_hr ON employment_creation_audit FOR ALL
    USING (public.olarize_can_write_employee(company_id))
    WITH CHECK (public.olarize_can_write_employee(company_id));
CREATE POLICY employee_request_hr ON employment_creation_request FOR ALL
    USING (public.olarize_can_write_employee(company_id))
    WITH CHECK (public.olarize_can_write_employee(company_id));

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT SELECT, INSERT ON employment_employee, employment_episode,
            employment_creation_audit, employment_creation_request TO olarize_runtime;
        GRANT EXECUTE ON FUNCTION public.olarize_can_write_employee(uuid) TO olarize_runtime;
    END IF;
END;
$$;

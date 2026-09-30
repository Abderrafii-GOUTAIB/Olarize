-- Tie each contract to one employee episode within the same company. Composite
-- references remain safe even though PostgreSQL FK checks do not apply RLS.
ALTER TABLE employment_episode
    ADD CONSTRAINT employment_episode_id_employee_company_unique UNIQUE (id, employee_id, company_id);

CREATE TABLE employment_contract (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    episode_id uuid NOT NULL,
    current_version bigint NOT NULL CHECK (current_version >= 1),
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT employment_contract_id_company_unique UNIQUE (id, company_id),
    CONSTRAINT employment_contract_employee_fk FOREIGN KEY (employee_id, company_id)
        REFERENCES employment_employee(id, company_id),
    CONSTRAINT employment_contract_episode_fk FOREIGN KEY (episode_id, employee_id, company_id)
        REFERENCES employment_episode(id, employee_id, company_id)
);
CREATE INDEX employment_contract_company_idx ON employment_contract(company_id);

CREATE TABLE employment_contract_version (
    id uuid PRIMARY KEY,
    contract_id uuid NOT NULL,
    company_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    episode_id uuid NOT NULL,
    version bigint NOT NULL CHECK (version >= 1),
    type_code varchar(32) NOT NULL CHECK (type_code IN ('CDI', 'CDD', 'SPECIFIED_WORK', 'UNKNOWN')),
    start_date date NOT NULL,
    end_date date,
    source_reference varchar(200) NOT NULL,
    review_state varchar(32) NOT NULL CHECK (review_state = 'UNREVIEWED'),
    actor_subject varchar(255) NOT NULL,
    operation varchar(16) NOT NULL CHECK (operation IN ('CREATE', 'CORRECT')),
    change_reason varchar(1000),
    evidence_reference varchar(200),
    correlation_id varchar(64),
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_contract_version_contract_fk FOREIGN KEY (contract_id, company_id)
        REFERENCES employment_contract(id, company_id),
    CONSTRAINT employment_contract_version_episode_fk FOREIGN KEY (episode_id, employee_id, company_id)
        REFERENCES employment_episode(id, employee_id, company_id),
    CONSTRAINT employment_contract_version_number_unique UNIQUE (contract_id, version),
    CONSTRAINT employment_contract_version_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT employment_contract_version_audit CHECK (
        (operation = 'CREATE' AND version = 1 AND change_reason IS NULL AND evidence_reference IS NULL)
        OR (operation = 'CORRECT' AND version > 1 AND coalesce(length(btrim(change_reason)), 0) > 0
            AND coalesce(length(btrim(evidence_reference)), 0) > 0)
    )
);
CREATE INDEX employment_contract_version_company_idx ON employment_contract_version(company_id, contract_id, version);

CREATE TABLE employment_contract_request (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    actor_subject varchar(255) NOT NULL,
    operation varchar(16) NOT NULL CHECK (operation IN ('CREATE', 'CORRECT')),
    idempotency_key uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    contract_id uuid NOT NULL,
    result_version bigint NOT NULL,
    recorded_at timestamptz NOT NULL,
    CONSTRAINT employment_contract_request_contract_fk FOREIGN KEY (contract_id, company_id)
        REFERENCES employment_contract(id, company_id),
    CONSTRAINT employment_contract_request_version_fk FOREIGN KEY (contract_id, result_version)
        REFERENCES employment_contract_version(contract_id, version),
    CONSTRAINT employment_contract_request_key_unique UNIQUE (company_id, actor_subject, operation, idempotency_key)
);
CREATE INDEX employment_contract_request_company_idx ON employment_contract_request(company_id);

ALTER TABLE employment_contract ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_contract FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_contract_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_contract_version FORCE ROW LEVEL SECURITY;
ALTER TABLE employment_contract_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_contract_request FORCE ROW LEVEL SECURITY;

-- RLS confirms both signed selected-company scope and the actor's current
-- contract.write permission for each contract fact, version, and retry receipt.
CREATE FUNCTION public.olarize_can_write_contract(p_company uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, public AS $$
    SELECT p_company::text = current_setting('olarize.company_id', true)
       AND public.olarize_verified_subject() IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.identity_membership m
                   WHERE m.company_id = p_company
                     AND m.subject = public.olarize_verified_subject()
                     AND m.status = 'ACTIVE'
                     AND m.permissions @> ARRAY['contract.write']::text[])
$$;
REVOKE ALL ON FUNCTION public.olarize_can_write_contract(uuid) FROM PUBLIC;

CREATE POLICY contract_write ON employment_contract FOR ALL
    USING (public.olarize_can_write_contract(company_id))
    WITH CHECK (public.olarize_can_write_contract(company_id));
CREATE POLICY contract_version_write ON employment_contract_version FOR ALL
    USING (public.olarize_can_write_contract(company_id))
    WITH CHECK (public.olarize_can_write_contract(company_id));
CREATE POLICY contract_request_write ON employment_contract_request FOR ALL
    USING (public.olarize_can_write_contract(company_id))
    WITH CHECK (public.olarize_can_write_contract(company_id));

DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE ON employment_contract TO olarize_runtime;
        GRANT SELECT, INSERT ON employment_contract_version, employment_contract_request TO olarize_runtime;
        GRANT EXECUTE ON FUNCTION public.olarize_can_write_contract(uuid) TO olarize_runtime;
    END IF;
END;
$$;

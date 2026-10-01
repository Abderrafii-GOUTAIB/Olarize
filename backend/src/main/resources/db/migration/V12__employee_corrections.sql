-- Versioned factual corrections, restricted history, and minimal dated job projection.
ALTER TABLE employment_employee DROP CONSTRAINT employment_employee_version_check;
ALTER TABLE employment_employee ADD CONSTRAINT employment_employee_version_check CHECK (version >= 1);
ALTER TABLE employment_episode DROP CONSTRAINT employment_episode_version_check;
ALTER TABLE employment_episode ADD CONSTRAINT employment_episode_version_check CHECK (version >= 1);
ALTER TABLE employment_episode ADD CONSTRAINT employment_episode_company_id_unique UNIQUE (id, employee_id, company_id);

CREATE TABLE employment_employee_version (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    episode_id uuid NOT NULL,
    version bigint NOT NULL CHECK (version >= 1),
    episode_version bigint NOT NULL CHECK (episode_version >= 1),
    reference varchar(64) NOT NULL,
    normalized_reference varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    start_date date NOT NULL,
    job_title varchar(200), department varchar(200), site varchar(200),
    work_email varchar(254), work_phone varchar(32),
    effective_date date NOT NULL,
    job_changed boolean NOT NULL,
    actor_subject varchar(255), reason varchar(1000), correlation_id varchar(64),
    recorded_at timestamptz NOT NULL,
    idempotency_key uuid, payload_hash varchar(64),
    CONSTRAINT employee_version_employee_fk FOREIGN KEY (employee_id, company_id)
        REFERENCES employment_employee(id, company_id),
    CONSTRAINT employee_version_episode_fk FOREIGN KEY (episode_id, employee_id, company_id)
        REFERENCES employment_episode(id, employee_id, company_id),
    CONSTRAINT employee_version_unique UNIQUE (company_id, employee_id, version),
    CONSTRAINT employee_correction_key_unique UNIQUE (company_id, actor_subject, idempotency_key),
    CONSTRAINT employee_correction_audit CHECK (version = 1 OR
        (actor_subject IS NOT NULL AND reason IS NOT NULL AND length(trim(reason)) > 0
            AND idempotency_key IS NOT NULL AND payload_hash IS NOT NULL))
);
CREATE INDEX employee_version_episode_idx ON employment_employee_version(company_id, episode_id);

CREATE TABLE employment_job_version (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL,
    episode_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    source_version bigint NOT NULL CHECK (source_version >= 1),
    effective_date date NOT NULL,
    job_title varchar(200), department varchar(200), site varchar(200),
    CONSTRAINT employee_job_episode_fk FOREIGN KEY (episode_id, employee_id, company_id)
        REFERENCES employment_episode(id, employee_id, company_id),
    CONSTRAINT employee_job_version_unique UNIQUE (company_id, episode_id, source_version)
);
CREATE INDEX employee_job_date_idx ON employment_job_version(company_id, episode_id, effective_date, source_version);

-- Migration role can backfill existing factual baselines. Later local seeds are captured
-- on their first correction; runtime cannot alter any immutable version after insertion.
INSERT INTO employment_employee_version
    (id, company_id, employee_id, episode_id, version, episode_version, reference,
     normalized_reference, name, start_date, job_title, department, site, work_email,
     work_phone, effective_date, job_changed, recorded_at)
SELECT gen_random_uuid(), e.company_id, e.id, ep.id, e.version, ep.version, e.reference,
       e.normalized_reference, e.name, ep.start_date, e.job_title, e.department, e.site,
       e.work_email, e.work_phone, ep.start_date, false, e.recorded_at
FROM employment_employee e JOIN employment_episode ep ON ep.employee_id = e.id AND ep.company_id = e.company_id;
INSERT INTO employment_job_version (id, company_id, episode_id, employee_id, source_version,
    effective_date, job_title, department, site)
SELECT gen_random_uuid(), e.company_id, ep.id, e.id, e.version, ep.start_date, e.job_title, e.department, e.site
FROM employment_employee e JOIN employment_episode ep ON ep.employee_id = e.id AND ep.company_id = e.company_id
WHERE e.job_title IS NOT NULL OR e.department IS NOT NULL OR e.site IS NOT NULL;

ALTER TABLE employment_employee_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_employee_version FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_version_read ON employment_employee_version FOR SELECT
    USING (public.olarize_can_write_employee(company_id));
CREATE POLICY employee_version_insert ON employment_employee_version FOR INSERT
    WITH CHECK (public.olarize_can_write_employee(company_id));
ALTER TABLE employment_job_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_job_version FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_job_read ON employment_job_version FOR SELECT
    USING (public.olarize_can_write_employee(company_id) OR public.olarize_can_read_employee(company_id));
CREATE POLICY employee_job_insert ON employment_job_version FOR INSERT
    WITH CHECK (public.olarize_can_write_employee(company_id));

DO $$ BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT UPDATE (reference, normalized_reference, name, work_email, work_phone, version, recorded_at)
            ON employment_employee TO olarize_runtime;
        GRANT UPDATE (start_date, version, recorded_at) ON employment_episode TO olarize_runtime;
        GRANT SELECT, INSERT ON employment_employee_version, employment_job_version TO olarize_runtime;
    END IF;
END $$;

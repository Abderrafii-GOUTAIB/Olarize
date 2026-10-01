-- Persistent synthetic baselines for local Swagger; never loaded by dev/prod.
INSERT INTO employment_employee_version
    (id, company_id, employee_id, episode_id, version, episode_version, reference,
     normalized_reference, name, start_date, job_title, department, site, work_email,
     work_phone, effective_date, job_changed, actor_subject, recorded_at)
SELECT gen_random_uuid(), e.company_id, e.id, ep.id, e.version, ep.version, e.reference,
       e.normalized_reference, e.name, ep.start_date, e.job_title, e.department, e.site,
       e.work_email, e.work_phone, ep.start_date, false, 'local-demo-seed', e.recorded_at
FROM employment_employee e JOIN employment_episode ep ON ep.employee_id = e.id AND ep.company_id = e.company_id
WHERE e.company_id = 'c0000000-0000-4000-8000-000000000001' AND e.version = 1
ON CONFLICT (company_id, employee_id, version) DO NOTHING;
INSERT INTO employment_job_version (id, company_id, episode_id, employee_id, source_version,
    effective_date, job_title, department, site)
SELECT gen_random_uuid(), e.company_id, ep.id, e.id, e.version, ep.start_date, e.job_title, e.department, e.site
FROM employment_employee e JOIN employment_episode ep ON ep.employee_id = e.id AND ep.company_id = e.company_id
WHERE e.company_id = 'c0000000-0000-4000-8000-000000000001' AND e.version = 1
    AND (e.job_title IS NOT NULL OR e.department IS NOT NULL OR e.site IS NOT NULL)
ON CONFLICT (company_id, episode_id, source_version) DO NOTHING;

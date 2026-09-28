CREATE TABLE organization_company (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL CHECK (length(btrim(name)) > 0),
    version bigint NOT NULL DEFAULT 1 CHECK (version = 1),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL CHECK (updated_at = created_at)
);

-- Creation evidence doubles as the immutable retry receipt; no duplicate payload store.
CREATE TABLE organization_company_creation (
    company_id uuid PRIMARY KEY REFERENCES organization_company(id),
    operator_name text NOT NULL,
    idempotency_key uuid NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(btrim(reason)) > 0),
    action text NOT NULL CHECK (action = 'COMPANY_CREATED'),
    recorded_at timestamptz NOT NULL,
    correlation_id uuid NOT NULL,
    UNIQUE (operator_name, idempotency_key)
);

REVOKE ALL ON organization_company, organization_company_creation FROM PUBLIC;
ALTER TABLE organization_company ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_company FORCE ROW LEVEL SECURITY;
ALTER TABLE organization_company_creation ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_company_creation FORCE ROW LEVEL SECURITY;

-- Provisioning role membership is checked by the command. Database privileges are
-- granted separately by an operator; no web/runtime grant is made by migration.
-- Rows are visible during a command only for its transaction's selected company.
CREATE POLICY company_scoped_read ON organization_company FOR SELECT
    USING (id::text = current_setting('olarize.company_id', true));
CREATE POLICY company_provision_insert ON organization_company FOR INSERT
    WITH CHECK (id::text = current_setting('olarize.company_id', true));
CREATE POLICY creation_own_read ON organization_company_creation FOR SELECT
    USING (operator_name = session_user);
CREATE POLICY creation_own_insert ON organization_company_creation FOR INSERT
    WITH CHECK (operator_name = session_user
        AND company_id::text = current_setting('olarize.company_id', true));

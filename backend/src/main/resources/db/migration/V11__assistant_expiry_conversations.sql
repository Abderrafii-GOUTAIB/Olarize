-- Metadata only: questions and HR results are never persisted here.
CREATE TABLE assistant_conversation (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES organization_company(id),
    owner_user_id text NOT NULL CHECK (length(owner_user_id) BETWEEN 1 AND 255),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at > created_at),
    UNIQUE (company_id, id)
);
ALTER TABLE assistant_conversation ENABLE ROW LEVEL SECURITY;
ALTER TABLE assistant_conversation FORCE ROW LEVEL SECURITY;
CREATE POLICY assistant_conversation_owned_read ON assistant_conversation FOR SELECT
    USING (public.olarize_can_read_contract(company_id)
       AND owner_user_id = public.olarize_verified_subject() AND expires_at > now());
CREATE POLICY assistant_conversation_owned_create ON assistant_conversation FOR INSERT
    WITH CHECK (public.olarize_can_read_contract(company_id)
       AND owner_user_id = public.olarize_verified_subject()
       AND expires_at > now() AND expires_at <= created_at + interval '24 hours');
CREATE INDEX assistant_conversation_expiry_idx ON assistant_conversation(expires_at);
DO $$
BEGIN
    IF to_regrole('olarize_runtime') IS NOT NULL THEN
        GRANT SELECT, INSERT ON assistant_conversation TO olarize_runtime;
    END IF;
END;
$$;

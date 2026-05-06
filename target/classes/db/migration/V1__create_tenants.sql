-- V1: Tenant (firma) tablosu — public schema
CREATE TABLE IF NOT EXISTS public.tenants (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name  VARCHAR(255) NOT NULL UNIQUE,
    schema_name   VARCHAR(100) NOT NULL UNIQUE,
    plan          VARCHAR(50)  NOT NULL DEFAULT 'STARTER',
    max_branches  INTEGER      NOT NULL DEFAULT 3,
    max_employees INTEGER      NOT NULL DEFAULT 50,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- updated_at otomatik güncellensin
CREATE OR REPLACE FUNCTION public.update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tenants_updated_at
    BEFORE UPDATE ON public.tenants
    FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

-- Demo tenant
INSERT INTO public.tenants
    (company_name, schema_name, plan, contact_email)
VALUES
    ('Demo Firma A.Ş.', 'schema_demo', 'PROFESSIONAL', 'admin@demo.com')
ON CONFLICT DO NOTHING;

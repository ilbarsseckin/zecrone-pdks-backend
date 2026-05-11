-- ============================================================
-- V2: E-posta doğrulama sistemi (public schema)
-- ============================================================

-- Tenants tablosuna doğrulama alanları ekle
ALTER TABLE public.tenants
    ADD COLUMN IF NOT EXISTS verified          BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS verified_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS trial_started_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS trial_ends_at     TIMESTAMP;

-- E-posta doğrulama token tablosu
CREATE TABLE IF NOT EXISTS public.email_verification_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    token       UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evt_token     ON public.email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_evt_tenant_id ON public.email_verification_tokens(tenant_id);

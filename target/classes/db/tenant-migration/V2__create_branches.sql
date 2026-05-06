-- V2: Şube tablosu — tenant schema'sına kurulur
CREATE TABLE IF NOT EXISTS branches (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(100),
    address         TEXT,
    phone           VARCHAR(20),
    work_start      TIME        NOT NULL DEFAULT '08:00',
    work_end        TIME        NOT NULL DEFAULT '17:00',
    late_tolerance  INTEGER     NOT NULL DEFAULT 5,
    timezone        VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER branches_updated_at
    BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

-- Demo şube
INSERT INTO branches (name, city, work_start, work_end)
VALUES ('Merkez Şube', 'İstanbul', '08:00', '17:00')
ON CONFLICT DO NOTHING;

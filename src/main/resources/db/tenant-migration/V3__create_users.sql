-- V3: Kullanıcı tablosu
CREATE TABLE IF NOT EXISTS users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    branch_id     UUID REFERENCES branches(id),
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- V4: Personel tablosu
CREATE TABLE IF NOT EXISTS employees (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id   UUID        NOT NULL REFERENCES branches(id),
    user_id     UUID        REFERENCES users(id),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(20),
    department  VARCHAR(100),
    position    VARCHAR(100),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

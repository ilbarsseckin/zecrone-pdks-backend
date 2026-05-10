CREATE TABLE IF NOT EXISTS leaves (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    employee_id uuid NOT NULL,
    type varchar(50) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    total_days int NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    description varchar(500),
    requested_by varchar(100),
    reviewed_by uuid,
    review_note varchar(500),
    reviewed_at timestamp,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS leave_balances (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    employee_id uuid NOT NULL,
    year int NOT NULL,
    entitled_days int NOT NULL DEFAULT 14,
    used_days int NOT NULL DEFAULT 0,
    pending_days int NOT NULL DEFAULT 0
);
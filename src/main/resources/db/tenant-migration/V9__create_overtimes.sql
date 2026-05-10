CREATE TABLE IF NOT EXISTS overtimes (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    employee_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    overtime_date date NOT NULL,
    hours numeric(4,2) NOT NULL,
    type varchar(20) NOT NULL DEFAULT 'WEEKDAY',
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    note varchar(500),
    reviewed_by uuid,
    reviewed_at timestamp,
    created_at timestamp NOT NULL DEFAULT now()
);
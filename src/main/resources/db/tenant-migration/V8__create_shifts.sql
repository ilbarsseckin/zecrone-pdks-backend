CREATE TABLE IF NOT EXISTS shifts (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    employee_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    name varchar(100),
    shift_date date NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    type varchar(20) NOT NULL DEFAULT 'MORNING',
    created_at timestamp NOT NULL DEFAULT now()
);
-- V6: Performans indeksleri
CREATE INDEX IF NOT EXISTS idx_employees_branch
    ON employees(branch_id);

CREATE INDEX IF NOT EXISTS idx_employees_status
    ON employees(status);

CREATE INDEX IF NOT EXISTS idx_attendance_date
    ON attendance_records(work_date);

CREATE INDEX IF NOT EXISTS idx_attendance_employee
    ON attendance_records(employee_id, work_date);

CREATE INDEX IF NOT EXISTS idx_attendance_branch_date
    ON attendance_records(branch_id, work_date);

CREATE INDEX IF NOT EXISTS idx_users_email
    ON users(email);

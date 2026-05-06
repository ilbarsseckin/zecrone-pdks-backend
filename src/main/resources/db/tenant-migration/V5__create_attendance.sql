-- V5: Yoklama kayıtları
CREATE TABLE IF NOT EXISTS attendance_records (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID      NOT NULL REFERENCES employees(id),
    branch_id    UUID      NOT NULL REFERENCES branches(id),
    work_date    DATE      NOT NULL,
    check_in     TIMESTAMP,
    check_out    TIMESTAMP,
    work_minutes INTEGER   NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    notes        TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(employee_id, work_date)
);

-- check_out girilince work_minutes otomatik hesapla
CREATE OR REPLACE FUNCTION calc_work_minutes()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.check_out IS NOT NULL AND NEW.check_in IS NOT NULL THEN
        NEW.work_minutes = EXTRACT(EPOCH FROM
            (NEW.check_out - NEW.check_in)) / 60;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calc_minutes_trigger
    BEFORE INSERT OR UPDATE ON attendance_records
    FOR EACH ROW EXECUTE FUNCTION calc_work_minutes();

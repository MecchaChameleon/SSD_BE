ALTER TABLE reservation
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';

UPDATE reservation SET payment_status = 'PAID'
WHERE status = 'COMPLETED' AND visit_start_at IS NULL;

COMMENT ON COLUMN reservation.payment_status IS 'UNPAID / PAID';

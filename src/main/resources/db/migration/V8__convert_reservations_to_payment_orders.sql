-- Reservation-only records are retired. The existing table remains as the
-- payment-order ledger to preserve foreign keys and historical data.
UPDATE reservation
SET status = 'CANCELED',
    hidden_by_buyer = TRUE,
    canceled_at = COALESCE(canceled_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE payment_status = 'UNPAID'
  AND status IN ('REQUESTED', 'APPROVED');

UPDATE reservation
SET payment_status = 'REFUNDED',
    canceled_at = COALESCE(canceled_at, rejected_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'REJECTED'
  AND payment_status = 'PAID';

COMMENT ON COLUMN reservation.status IS 'Payment order: REQUESTED(pending seller confirmation) / COMPLETED(accepted) / REJECTED(refunded); legacy statuses may remain';
COMMENT ON COLUMN reservation.payment_status IS 'PAID / REFUNDED; UNPAID is legacy only';

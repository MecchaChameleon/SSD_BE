ALTER TABLE reservation ADD COLUMN IF NOT EXISTS hidden_by_buyer BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS notification_setting (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    common_event BOOLEAN NOT NULL DEFAULT TRUE,
    seller_reservation BOOLEAN NOT NULL DEFAULT TRUE,
    seller_ai_price BOOLEAN NOT NULL DEFAULT TRUE,
    seller_settlement BOOLEAN NOT NULL DEFAULT TRUE,
    buyer_deadline BOOLEAN NOT NULL DEFAULT TRUE,
    buyer_reservation_approved BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reservation_status ON reservation(status);
CREATE INDEX IF NOT EXISTS idx_product_deadline ON product(reservation_close_at);

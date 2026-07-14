CREATE TABLE seller_settlement (
    id BIGSERIAL PRIMARY KEY,
    seller_profile_id BIGINT NOT NULL REFERENCES seller_profile(id),
    gross_amount BIGINT NOT NULL,
    platform_fee BIGINT NOT NULL,
    payment_fee BIGINT NOT NULL,
    settlement_amount BIGINT NOT NULL,
    bank_name VARCHAR(100),
    account_number VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE seller_settlement_reservation (
    settlement_id BIGINT NOT NULL REFERENCES seller_settlement(id) ON DELETE CASCADE,
    reservation_id BIGINT NOT NULL REFERENCES reservation(id),
    PRIMARY KEY (settlement_id, reservation_id),
    UNIQUE (reservation_id)
);

CREATE INDEX idx_seller_settlement_profile ON seller_settlement(seller_profile_id, requested_at DESC);

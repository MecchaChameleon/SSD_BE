ALTER TABLE product
    ADD COLUMN ai_auto_pricing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ai_last_priced_at TIMESTAMPTZ,
    ADD COLUMN ai_model_version VARCHAR(100);

CREATE INDEX idx_product_ai_auto_pricing
    ON product (status, ai_auto_pricing_enabled)
    WHERE ai_auto_pricing_enabled = TRUE;

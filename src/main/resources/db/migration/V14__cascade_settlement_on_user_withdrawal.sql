ALTER TABLE seller_settlement DROP CONSTRAINT seller_settlement_seller_profile_id_fkey;
ALTER TABLE seller_settlement ADD CONSTRAINT seller_settlement_seller_profile_id_fkey
    FOREIGN KEY (seller_profile_id) REFERENCES seller_profile(id) ON DELETE CASCADE;

ALTER TABLE seller_settlement_reservation DROP CONSTRAINT seller_settlement_reservation_reservation_id_fkey;
ALTER TABLE seller_settlement_reservation ADD CONSTRAINT seller_settlement_reservation_reservation_id_fkey
    FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE CASCADE;

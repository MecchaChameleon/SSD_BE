ALTER TABLE seller_application
    ADD COLUMN open_date VARCHAR(8),
    ADD COLUMN nts_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN nts_business_status VARCHAR(20);

COMMENT ON COLUMN seller_application.open_date IS '개업일자 YYYYMMDD';
COMMENT ON COLUMN seller_application.nts_verified_at IS '국세청 진위확인 통과 시각';
COMMENT ON COLUMN seller_application.nts_business_status IS '국세청 조회 사업자 상태명 (계속사업자/휴업자/폐업자)';

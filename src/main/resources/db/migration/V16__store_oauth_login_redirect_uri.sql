ALTER TABLE oauth_login_state
    ADD COLUMN login_redirect_uri VARCHAR(2048);

UPDATE oauth_login_state
SET login_redirect_uri = 'jejulocaltime://oauth/kakao'
WHERE login_redirect_uri IS NULL;

ALTER TABLE oauth_login_state
    ALTER COLUMN login_redirect_uri SET NOT NULL;

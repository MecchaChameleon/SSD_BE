CREATE TABLE oauth_login_state (
    state_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE login_ticket (
    ticket_hash VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_new_user BOOLEAN NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_oauth_login_state_expires_at ON oauth_login_state(expires_at);
CREATE INDEX idx_login_ticket_expires_at ON login_ticket(expires_at);

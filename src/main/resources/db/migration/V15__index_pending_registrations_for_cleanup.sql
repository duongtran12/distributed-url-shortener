CREATE INDEX idx_users_pending_registration_created
    ON users (created_at, id)
    WHERE enabled = FALSE;

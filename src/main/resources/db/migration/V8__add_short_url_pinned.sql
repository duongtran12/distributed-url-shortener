ALTER TABLE short_urls
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_short_urls_user_pinned
    ON short_urls (user_id, pinned);

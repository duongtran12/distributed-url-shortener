ALTER TABLE short_urls
    ADD COLUMN tag VARCHAR(32);

CREATE INDEX idx_short_urls_user_tag
    ON short_urls (user_id, tag);

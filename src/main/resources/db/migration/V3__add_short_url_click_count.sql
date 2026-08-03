ALTER TABLE short_urls
    ADD COLUMN click_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE short_urls
    ADD CONSTRAINT ck_short_urls_click_count
        CHECK (click_count >= 0);

CREATE TABLE click_events (
    event_id UUID PRIMARY KEY,
    short_code VARCHAR(32) NOT NULL,
    clicked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_click_events_short_code_clicked_at
    ON click_events (short_code, clicked_at DESC);

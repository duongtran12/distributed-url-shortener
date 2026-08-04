ALTER TABLE click_events
    ADD COLUMN visitor_hash CHAR(64);

CREATE INDEX idx_click_events_short_code_visitor_hash
    ON click_events (short_code, visitor_hash);

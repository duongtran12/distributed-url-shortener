CREATE INDEX idx_click_events_clicked_at
    ON click_events (clicked_at, event_id);

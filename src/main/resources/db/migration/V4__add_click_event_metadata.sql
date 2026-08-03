ALTER TABLE click_events
    ADD COLUMN user_agent VARCHAR(512),
    ADD COLUMN referrer VARCHAR(255) NOT NULL DEFAULT 'direct',
    ADD COLUMN browser VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN operating_system VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN device_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE click_events
    ADD CONSTRAINT ck_click_events_device_type
        CHECK (device_type IN ('DESKTOP', 'MOBILE', 'TABLET', 'BOT', 'UNKNOWN'));

CREATE INDEX idx_click_events_short_code_browser
    ON click_events (short_code, browser);

CREATE INDEX idx_click_events_short_code_device_type
    ON click_events (short_code, device_type);

CREATE INDEX idx_click_events_short_code_referrer
    ON click_events (short_code, referrer);

ALTER TABLE short_urls
    ADD COLUMN title VARCHAR(120);

ALTER TABLE short_urls
    ADD CONSTRAINT ck_short_urls_title_not_blank
        CHECK (title IS NULL OR BTRIM(title) <> '');

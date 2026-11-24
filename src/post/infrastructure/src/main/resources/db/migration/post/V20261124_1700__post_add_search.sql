CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE post
    ADD COLUMN title_normalized VARCHAR(40)
        GENERATED ALWAYS AS (lower(regexp_replace(title, '\s', '', 'g'))) STORED;

CREATE INDEX idx_post_title_normalized_trgm ON post USING gin (title_normalized gin_trgm_ops)
    WHERE status = 'AVAILABLE';

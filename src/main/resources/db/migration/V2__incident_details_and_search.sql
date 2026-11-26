ALTER TABLE incidents
    ADD COLUMN title VARCHAR(50),
    ADD COLUMN address VARCHAR(100),
    ADD COLUMN occurred_at TIMESTAMPTZ;

UPDATE incidents SET occurred_at = created_at;
ALTER TABLE incidents ALTER COLUMN occurred_at SET NOT NULL;

CREATE INDEX ix_incidents_occurred_at ON incidents (occurred_at DESC);
CREATE INDEX ix_incidents_location ON incidents (latitude, longitude);
CREATE INDEX ix_incidents_author ON incidents (author_id, created_at DESC);

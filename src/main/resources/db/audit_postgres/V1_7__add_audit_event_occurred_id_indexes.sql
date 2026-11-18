-- CONCURRENTLY builds each index without holding a lock that blocks inserts on the
-- live ingestion tables. Flyway detects these statements and runs them outside a
-- transaction automatically, provided the migration contains only such statements.
CREATE INDEX CONCURRENTLY IF NOT EXISTS staff_occurred_id_idx
    ON staff.audit_event (occurred, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS prisoner_occurred_id_idx
    ON prisoner.audit_event (occurred, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS person_on_probation_occurred_id_idx
    ON person_on_probation.audit_event (occurred, id);

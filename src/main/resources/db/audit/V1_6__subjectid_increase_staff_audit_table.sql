ALTER TABLE staff.audit_event
    ALTER COLUMN subject_id TYPE VARCHAR(200),
    ALTER COLUMN subject_type  TYPE VARCHAR(200);

ALTER TABLE prisoner.audit_event
    ALTER COLUMN subject_id TYPE VARCHAR(200),
    ALTER COLUMN subject_type  TYPE VARCHAR(200);

ALTER TABLE person_on_probation.audit_event
    ALTER COLUMN subject_id TYPE VARCHAR(200),
    ALTER COLUMN subject_type  TYPE VARCHAR(200);

ALTER TABLE audit_event
    ADD COLUMN subject_id varchar(80);

ALTER TABLE audit_event
    ADD COLUMN subject_type varchar(100);

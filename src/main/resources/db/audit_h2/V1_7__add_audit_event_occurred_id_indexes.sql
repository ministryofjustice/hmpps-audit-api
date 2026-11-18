CREATE INDEX staff_occurred_id_idx
    ON staff.audit_event (occurred, id);

CREATE INDEX prisoner_occurred_id_idx
    ON prisoner.audit_event (occurred, id);

CREATE INDEX person_on_probation_occurred_id_idx
    ON person_on_probation.audit_event (occurred, id);

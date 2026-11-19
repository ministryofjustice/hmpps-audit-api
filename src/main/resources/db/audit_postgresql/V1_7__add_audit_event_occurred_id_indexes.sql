create index concurrently if not exists staff_occurred_id_idx
    on staff.audit_event (occurred, id);

create index concurrently if not exists prisoner_occurred_id_idx
    on prisoner.audit_event (occurred, id);

create index concurrently if not exists person_on_probation_occurred_id_idx
    on person_on_probation.audit_event (occurred, id);

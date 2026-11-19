create table person_on_probation.audit_event
(
    id              uuid primary key,
    operation_id    varchar(80),
    correlation_id  varchar(80),
    what            varchar(200) not null,
    occurred        timestamp with time zone not null,
    who             varchar(80),
    subject_id      varchar(80),
    subject_type    varchar(100),
    service         varchar(200),
    details         json
);


create index person_on_probation_who_idx
    on person_on_probation.audit_event (who);

create index person_on_probation_what_idx
    on person_on_probation.audit_event (what);

create index person_on_probation_subject_id_idx
    on person_on_probation.audit_event (subject_id);

create index person_on_probation_service
    on person_on_probation.audit_event (service);
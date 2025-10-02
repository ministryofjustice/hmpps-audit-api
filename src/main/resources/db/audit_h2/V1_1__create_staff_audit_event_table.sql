create table staff.audit_event
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
    details         varchar(1000)
);

create index staff_who_idx
    on staff.audit_event (who);

create index staff_what_idx
    on staff.audit_event (what);

create index staff_subject_id_idx
    on staff.audit_event (subject_id);

create index staff_service
    on staff.audit_event (service);
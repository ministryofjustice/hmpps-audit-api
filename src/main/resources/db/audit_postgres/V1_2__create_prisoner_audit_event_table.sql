create table prisoner.audit_event
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


create index prisoner_who_idx
    on prisoner.audit_event (who);

create index prisoner_what_idx
    on prisoner.audit_event (what);

create index prisoner_subject_id_idx
    on prisoner.audit_event (subject_id);

create index prisoner_service
    on prisoner.audit_event (service);
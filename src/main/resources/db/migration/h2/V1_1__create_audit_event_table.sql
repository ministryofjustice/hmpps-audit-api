CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS prisoner;

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
    details         varchar(1000)
);
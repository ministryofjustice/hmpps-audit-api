create table audit_event
(
    id              uuid primary key,
    operation_id    varchar(80),
    what            varchar(200) not null,
    occurred        timestamp with time zone not null,
    who             varchar(80),
    service         varchar(200),
    details         json
);

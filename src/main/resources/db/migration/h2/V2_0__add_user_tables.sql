CREATE TABLE staff.audit_user
(
    id                 UUID PRIMARY KEY,
    creation_time      TIMESTAMP,
    last_modified_time TIMESTAMP
);


CREATE TABLE staff.auth_user_id
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      UUID         NOT NULL,
    user_id            VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP,
    FOREIGN KEY (audit_user_id) REFERENCES audit_user (id)
);

CREATE TABLE staff.auth_user_uuid
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      UUID      NOT NULL,
    user_uuid          UUID      NOT NULL,
    active             BOOLEAN   NOT NULL,
    creation_time      TIMESTAMP NOT NULL,
    last_modified_time TIMESTAMP,
    FOREIGN KEY (audit_user_id) REFERENCES audit_user (id)
);

CREATE TABLE staff.auth_email_address
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      UUID         NOT NULL,
    email_address      VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP,
    FOREIGN KEY (audit_user_id) REFERENCES audit_user (id)
);

CREATE TABLE staff.auth_username
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      UUID         NOT NULL,
    username           VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP,
    FOREIGN KEY (audit_user_id) REFERENCES audit_user (id)
);
